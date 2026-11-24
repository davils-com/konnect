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

package com.davils.konnect.ipc.core.framing

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcReadResult
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

private const val HEADER_SIZE: Int = 4
private const val BYTE_BITS: Int = 8
private const val BYTE_MASK: Long = 0xFF

internal class LengthPrefixedFraming(private val maxFrameSize: Int) : IpcFraming<ByteString> {
    init {
        require(maxFrameSize > 0) { "The largest frame must hold at least one byte" }
    }

    override suspend fun receive(connection: IpcConnection): IpcFrameResult<ByteString> =
        when (val header = connection.readExactly(HEADER_SIZE)) {
            is IpcReadResult.Received -> body(connection, sizeOf(header.bytes))
            is IpcReadResult.Closed -> IpcFrameResult.Closed
            is IpcReadResult.Failed -> IpcFrameResult.Failed(header.error)
        }

    override fun encode(frame: ByteString): ByteString {
        require(frame.size <= maxFrameSize) { "A frame must not exceed the largest frame of its framing" }
        return buildByteString(HEADER_SIZE + frame.size) {
            for (index in HEADER_SIZE - 1 downTo 0) append((frame.size shr index * BYTE_BITS).toByte())
            append(frame)
        }
    }

    override fun toString(): String = "LengthPrefixedFraming(maxFrameSize=$maxFrameSize)"

    private fun sizeOf(header: ByteString): Long =
        (0 until HEADER_SIZE).fold(0L) { size, index -> size shl BYTE_BITS or (header[index].toLong() and BYTE_MASK) }

    private suspend fun body(connection: IpcConnection, size: Long): IpcFrameResult<ByteString> {
        if (size > maxFrameSize) {
            connection.close()
            return IpcFrameResult.TooLarge(size = size, maxFrameSize = maxFrameSize)
        }
        return when (val body = connection.readExactly(size.toInt())) {
            is IpcReadResult.Received -> IpcFrameResult.Received(body.bytes)
            is IpcReadResult.Closed -> IpcFrameResult.Closed
            is IpcReadResult.Failed -> IpcFrameResult.Failed(body.error)
        }
    }
}
