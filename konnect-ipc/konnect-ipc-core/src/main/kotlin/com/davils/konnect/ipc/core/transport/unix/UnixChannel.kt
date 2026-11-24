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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.ipc.core.transport.unix

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.ipc.core.transport.toSystemError
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

/**
 * The channel of a connected Unix domain socket.
 *
 * Every blocking call of the socket runs on [dispatcher] and is interrupted when its coroutine is cancelled. The
 * peer is read once, when the channel is created; a peer whose user cannot be read is [IpcPeer.UNKNOWN].
 *
 * @param socket the connected socket; the channel owns it.
 * @param dispatcher the dispatcher the blocking calls run on, such as `Dispatchers.IO`.
 * @since 1.0.0
 */
@DavilsInternal
public class UnixChannel(
    private val socket: UnixSocket,
    private val dispatcher: CoroutineDispatcher,
) : IpcChannel {
    override val peer: IpcPeer = socket.peer()

    override suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead = try {
        val count = runInterruptible(dispatcher) { socket.read(ByteBuffer.wrap(target, offset, length)) }
        countOf(count)
    } catch (expectedClosed: ClosedChannelException) {
        IpcChannelRead.Ended
    } catch (expectedFailure: IOException) {
        IpcChannelRead.Failed(expectedFailure.toSystemError())
    }

    override suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult = try {
        runInterruptible(dispatcher) { writeFully(ByteBuffer.wrap(source, offset, length)) }
        IpcWriteResult.Written
    } catch (expectedClosed: ClosedChannelException) {
        IpcWriteResult.Closed
    } catch (expectedFailure: IOException) {
        IpcWriteResult.Failed(expectedFailure.toSystemError())
    }

    override suspend fun closeOutput(): IpcWriteResult = try {
        runInterruptible(dispatcher) { socket.shutdownOutput() }
        IpcWriteResult.Written
    } catch (expectedClosed: ClosedChannelException) {
        IpcWriteResult.Closed
    } catch (expectedFailure: IOException) {
        IpcWriteResult.Failed(expectedFailure.toSystemError())
    }

    override fun close() {
        try {
            socket.close()
        } catch (expectedFailure: IOException) {
            return
        }
    }

    private fun countOf(count: Int): IpcChannelRead {
        if (count < 0) return IpcChannelRead.Ended
        return IpcChannelRead.Read(count)
    }

    private fun writeFully(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) socket.write(buffer)
    }
}
