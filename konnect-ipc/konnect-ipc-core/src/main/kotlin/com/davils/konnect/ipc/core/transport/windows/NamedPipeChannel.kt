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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.blocking
import com.davils.kore.core.platform.NativeResult
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.ByteBuffer

/**
 * The channel of a connected named pipe.
 *
 * Every blocking call runs on [dispatcher]; a call whose coroutine is cancelled cancels the pending operation of
 * the pipe. The channel moves bytes through two direct buffers of [bufferSize] bytes each, taken when it is created.
 *
 * @param api the pipe operations.
 * @param handle the connected pipe; the channel owns it.
 * @property peer the process on the other side.
 * @param bufferSize the size of each buffer, in bytes, from [Limits.MIN_BUFFER_SIZE] to [Limits.MAX_BUFFER_SIZE].
 * @param dispatcher the dispatcher the blocking calls run on, such as `Dispatchers.IO`.
 * @throws IllegalArgumentException if [bufferSize] is outside of its bounds.
 * @since 1.0.0
 */
@DavilsInternal
public class NamedPipeChannel(
    private val api: NamedPipeApi,
    private val handle: PipeHandle,
    override val peer: IpcPeer,
    bufferSize: Int,
    private val dispatcher: CoroutineDispatcher,
) : IpcChannel {
    init {
        require(bufferSize in Limits.MIN_BUFFER_SIZE..Limits.MAX_BUFFER_SIZE) {
            "The buffer of a pipe must be from ${Limits.MIN_BUFFER_SIZE} to ${Limits.MAX_BUFFER_SIZE} bytes"
        }
    }

    private val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize)

    private val writeBuffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize)

    override suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead =
        handle.use(ifClosed = IpcChannelRead.Ended) { value ->
            val count = minOf(a = length, b = readBuffer.capacity())
            val result = blocking(dispatcher, onCancel = { api.cancel(value) }) { api.read(value, readBuffer, count) }
            readOutcome(result = result, target = target, offset = offset, requested = count)
        }

    override suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult =
        handle.use(ifClosed = IpcWriteResult.Closed) { value ->
            writeAll(value = value, source = source, offset = offset, length = length)
        }

    override suspend fun closeOutput(): IpcWriteResult = handle.use(ifClosed = IpcWriteResult.Closed) { value ->
        val result = blocking(dispatcher, onCancel = { api.cancel(value) }) { api.write(value, writeBuffer, 0) }
        outputClosed(result)
    }

    override fun close() {
        handle.close()
    }

    private fun readOutcome(result: NativeResult, target: ByteArray, offset: Int, requested: Int): IpcChannelRead {
        if (!result.isSuccess || result.value == 0L) return ended(result)
        val count = minOf(a = result.value, b = requested.toLong()).toInt()
        readBuffer.get(0, target, offset, count)
        return IpcChannelRead.Read(count)
    }

    private fun ended(result: NativeResult): IpcChannelRead {
        val code = result.errorCode
        if (result.isSuccess || code in Win32Error.endOfStream) return IpcChannelRead.Ended
        return IpcChannelRead.Failed(Win32Error.errorOf(code))
    }

    private suspend fun writeAll(value: Long, source: ByteArray, offset: Int, length: Int): IpcWriteResult {
        var written = 0
        while (written < length) {
            val count = minOf(a = length - written, b = writeBuffer.capacity())
            writeBuffer.put(0, source, offset + written, count)
            val result = blocking(dispatcher, onCancel = { api.cancel(value) }) { api.write(value, writeBuffer, count) }
            if (!result.isSuccess || result.value == 0L) return refused(result)
            written += minOf(a = result.value, b = count.toLong()).toInt()
        }
        return IpcWriteResult.Written
    }

    private fun outputClosed(result: NativeResult): IpcWriteResult {
        if (result.isSuccess) return IpcWriteResult.Written
        return refused(result)
    }

    private fun refused(result: NativeResult): IpcWriteResult {
        val code = result.errorCode
        if (result.isSuccess || code in Win32Error.endOfStream) return IpcWriteResult.Closed
        return IpcWriteResult.Failed(Win32Error.errorOf(code))
    }
}
