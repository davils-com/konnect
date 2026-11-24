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

package com.davils.konnect.ipc.server.http.connection

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcReadResult
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.ReaderJob
import io.ktor.utils.io.WriterJob
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.reader
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private val continueHead: ByteArray = "HTTP/1.1 100 Continue\r\n\r\n".encodeToByteArray()
private const val READ_FAILED: String = "The connection failed while it was read"
private const val WRITE_REFUSED: String = "The connection refused a write"

@OptIn(ExperimentalAtomicApi::class)
internal class ConnectionWire(private val connection: IpcConnection, scope: CoroutineScope) {
    private val refusal = AtomicReference<IOException?>(null)

    private val reading: WriterJob = scope.writer { received(channel) }

    private val sending: ReaderJob = scope.reader { sent(channel) }

    val input: ByteReadChannel get() = reading.channel

    val output: ByteWriteChannel get() = sending.channel

    suspend fun sendContinue() {
        output.writeFully(continueHead)
        output.flush()
    }

    suspend fun finish() {
        if (!output.isClosedForWrite) output.flushAndClose()
        sending.job.join()
        reading.job.cancel()
        val failure = refusal.load() ?: return
        throw failure
    }

    private suspend fun received(channel: ByteWriteChannel) {
        var result = connection.read()
        while (result is IpcReadResult.Received) {
            channel.writeByteArray(result.bytes.toByteArray())
            channel.flush()
            result = connection.read()
        }
        if (result is IpcReadResult.Failed) throw IOException(READ_FAILED)
    }

    private suspend fun sent(channel: ByteReadChannel) {
        val buffer = ByteArray(connection.bufferSize)
        var count = channel.readAvailable(buffer)
        while (count >= 0) {
            written(buffer, count)
            count = channel.readAvailable(buffer)
        }
    }

    private suspend fun written(buffer: ByteArray, count: Int) {
        val outcome = connection.write(ByteString(data = buffer, startIndex = 0, endIndex = count))
        if (outcome == IpcWriteResult.Written) return
        val failure = IOException(WRITE_REFUSED)
        refusal.store(failure)
        throw failure
    }
}
