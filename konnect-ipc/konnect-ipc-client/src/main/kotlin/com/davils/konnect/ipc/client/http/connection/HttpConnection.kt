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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.konnect.ipc.client.http.connection

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcReadResult
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.bytestring.ByteString
import java.io.IOException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val WRITE_REFUSED: String = "The connection refused a write"
private const val READ_FAILED: String = "The connection failed while it was read"

internal class HttpConnection(private val connection: IpcConnection, scope: CoroutineScope) {
    private val leased = AtomicBoolean(false)

    val input: ByteReadChannel = scope.writer { pump(channel) }.channel

    val isOpen: Boolean get() = connection.isOpen && !input.isClosedForRead

    val isQuiet: Boolean get() = input.availableForRead == 0

    fun lease() {
        leased.store(true)
    }

    fun endLease(): Boolean = leased.compareAndSet(expectedValue = true, newValue = false)

    suspend fun write(bytes: ByteArray) {
        val outcome = connection.write(ByteString(bytes))
        if (outcome != IpcWriteResult.Written) throw IOException(WRITE_REFUSED)
    }

    suspend fun closeOutput(): Boolean = connection.closeOutput() == IpcWriteResult.Written

    fun close() {
        connection.close()
        input.cancel(null)
    }

    private suspend fun pump(channel: ByteWriteChannel) {
        var result = connection.read()
        while (result is IpcReadResult.Received) {
            channel.writeByteArray(result.bytes.toByteArray())
            channel.flush()
            result = connection.read()
        }
        if (result is IpcReadResult.Failed) throw IOException(READ_FAILED)
    }
}
