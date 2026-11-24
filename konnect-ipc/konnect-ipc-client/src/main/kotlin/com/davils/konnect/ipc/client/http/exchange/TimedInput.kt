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

@file:OptIn(InternalAPI::class)

package com.davils.konnect.ipc.client.http.exchange

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.availableForRead
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Source

internal class TimedInput(
    private val source: ByteReadChannel,
    private val timeoutMillis: Long,
    private val timedOut: () -> Throwable,
) : ByteReadChannel {
    override val closedCause: Throwable? get() = source.closedCause

    override val isClosedForRead: Boolean get() = source.isClosedForRead

    @InternalAPI
    override val readBuffer: Source get() = source.readBuffer

    override suspend fun awaitContent(min: Int): Boolean {
        if (source.availableForRead >= min) return true
        return withTimeoutOrNull(timeoutMillis) { source.awaitContent(min) } ?: throw timedOut()
    }

    override fun cancel(cause: Throwable?) {
        source.cancel(cause)
    }

    companion object {
        fun of(source: ByteReadChannel, timeoutMillis: Long?, timedOut: () -> Throwable): ByteReadChannel {
            if (timeoutMillis == null) return source
            return TimedInput(source, timeoutMillis, timedOut)
        }
    }
}
