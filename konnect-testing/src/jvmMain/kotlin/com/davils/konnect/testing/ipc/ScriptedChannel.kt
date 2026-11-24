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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.awaitCancellation

/**
 * A channel that answers with the results it was given, for tests of what drives a channel.
 *
 * Each read takes the next of [reads]; a [IpcChannelRead.Read] copies up to its count of the next bytes of [data]
 * into the target. Once the reads are used up, a read waits until it is cancelled. Every write and every closing of
 * the output answers with [writeResult], or waits until it is cancelled when it is `null`. It is meant for one
 * coroutine at a time.
 *
 * @param peer the process on the other side.
 * @param reads the results of the reads, in order.
 * @param writeResult the result of every write, or `null` to wait for cancellation.
 * @param data the bytes the reads hand out, in order.
 * @since 1.0.0
 */
@DavilsInternal
public class ScriptedChannel(
    override val peer: IpcPeer = TestIpc.CLIENT,
    reads: List<IpcChannelRead> = emptyList(),
    private val writeResult: IpcWriteResult? = IpcWriteResult.Written,
    private val data: ByteArray = ByteArray(0),
) : IpcChannel {
    private val pendingReads = ArrayDeque(reads)

    private var served: Int = 0

    /**
     * How often the channel was closed.
     *
     * @since 1.0.0
     */
    public var closes: Int = 0
        private set

    /**
     * How often the output of the channel was closed.
     *
     * @since 1.0.0
     */
    public var outputCloses: Int = 0
        private set

    override suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead {
        val read = pendingReads.removeFirstOrNull() ?: awaitCancellation()
        if (read !is IpcChannelRead.Read) return read
        val available = minOf(a = read.count, b = data.size - served).coerceAtLeast(0)
        data.copyInto(
            destination = target,
            destinationOffset = offset,
            startIndex = served,
            endIndex = served + available,
        )
        served += available
        return read
    }

    override suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult =
        writeResult ?: awaitCancellation()

    override suspend fun closeOutput(): IpcWriteResult {
        outputCloses += 1
        return writeResult ?: awaitCancellation()
    }

    override fun close() {
        closes += 1
    }
}
