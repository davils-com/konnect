/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.konnect.ipc.server.http.exchange.request

import com.davils.kore.core.failure.attempted
import io.ktor.http.cio.decodeChunked
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

private const val CHUNK: Int = 8 * 1024

@OptIn(ExperimentalAtomicApi::class)
internal class RequestBody private constructor(
    val channel: ByteReadChannel,
    private val job: Job,
    private val timedOut: AtomicBoolean,
) {
    val isTimedOut: Boolean get() = timedOut.load()

    suspend fun finish(): Boolean {
        channel.cancel(null)
        job.join()
        return !job.isCancelled
    }

    companion object {
        fun start(
            framing: BodyFraming,
            input: ByteReadChannel,
            scope: CoroutineScope,
            limit: Long,
            timeout: Duration,
        ): RequestBody = when (framing) {
            BodyFraming.Empty -> RequestBody(ByteReadChannel.Empty, CompletableDeferred(Unit), AtomicBoolean(false))
            is BodyFraming.Length -> copied(scope, timeout) { body -> fixed(input, body, framing.bytes) }
            BodyFraming.Chunked -> copied(scope, timeout) { body -> chunked(input, body, limit) }
        }

        private fun copied(
            scope: CoroutineScope,
            timeout: Duration,
            copy: suspend (ByteChannel) -> Unit,
        ): RequestBody {
            val body = ByteChannel()
            val timedOut = AtomicBoolean(false)
            val job = scope.async { transferred(body = body, timeout = timeout, timedOut = timedOut, copy = copy) }
            job.invokeOnCompletion { cause -> cause?.let(body::cancel) }
            return RequestBody(body, job, timedOut)
        }

        private suspend fun transferred(
            body: ByteChannel,
            timeout: Duration,
            timedOut: AtomicBoolean,
            copy: suspend (ByteChannel) -> Unit,
        ) {
            val outcome = withTimeoutOrNull(timeout) { attempted { copy(body) } }
            if (outcome == null) {
                timedOut.store(true)
                throw BodyTimedOut()
            }
            val failure = outcome.exceptionOrNull()
            if (failure != null) throw failureOf(failure)
            body.flushAndClose()
        }

        private fun failureOf(failure: Throwable): Throwable {
            val isRead = failure is IOException && failure !is PayloadTooLargeException
            val isMalformed = isRead || failure is IllegalStateException
            if (isMalformed) return MalformedBody()
            return failure
        }

        private suspend fun fixed(input: ByteReadChannel, body: ByteChannel, length: Long) {
            val copied = pumped(source = input, target = body, limit = length)
            if (copied < length) throw MalformedBody()
        }

        private suspend fun chunked(input: ByteReadChannel, body: ByteChannel, limit: Long) = coroutineScope {
            val decoded = ByteChannel()
            launch { decodeChunked(input, decoded) }
            pumped(source = decoded, target = body, limit = limit)
            if (!decoded.exhausted()) throw PayloadTooLargeException(limit)
        }

        private suspend fun pumped(source: ByteReadChannel, target: ByteChannel, limit: Long): Long {
            val buffer = ByteArray(CHUNK)
            var copied = 0L
            var count = 0
            while (copied < limit && count >= 0) {
                val wanted = minOf(a = buffer.size.toLong(), b = limit - copied).toInt()
                count = source.readAvailable(buffer = buffer, offset = 0, length = wanted)
                if (count > 0) target.writeFully(value = buffer, startIndex = 0, endIndex = count)
                copied += count.coerceAtLeast(0)
            }
            target.flush()
            return copied
        }
    }
}
