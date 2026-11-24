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

package com.davils.konnect.ipc.core.transport

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.channels.Channel

internal class MemoryChannel(
    override val peer: IpcPeer,
    private val incoming: Channel<ByteArray>,
    private val outgoing: Channel<ByteArray>,
) : IpcChannel {
    private var pending: ByteArray = ByteArray(0)

    private var position: Int = 0

    override suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead {
        if (position == pending.size && !refilled()) return IpcChannelRead.Ended
        val count = minOf(a = length, b = pending.size - position)
        pending.copyInto(
            destination = target,
            destinationOffset = offset,
            startIndex = position,
            endIndex = position + count,
        )
        position += count
        return IpcChannelRead.Read(count)
    }

    override suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult {
        val sent = outgoing.trySend(source.copyOfRange(fromIndex = offset, toIndex = offset + length))
        if (sent.isClosed) return IpcWriteResult.Closed
        return IpcWriteResult.Written
    }

    override suspend fun closeOutput(): IpcWriteResult {
        if (!outgoing.close()) return IpcWriteResult.Closed
        return IpcWriteResult.Written
    }

    override fun close() {
        outgoing.close()
        incoming.cancel()
    }

    private suspend fun refilled(): Boolean {
        val received = incoming.receiveCatching()
        val next = received.getOrNull() ?: return false
        pending = next
        position = 0
        return true
    }
}
