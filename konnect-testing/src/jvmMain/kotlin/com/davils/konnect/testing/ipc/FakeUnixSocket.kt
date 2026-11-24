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

import com.davils.konnect.ipc.core.transport.unix.UnixSocket
import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

/**
 * A connected Unix socket that reads, writes and fails as it was told.
 *
 * Each read takes the next of [reads] and advances the buffer by it; once they are used up, a read throws
 * [readFailure]. A write takes at most [writeChunk] bytes. It is meant for one thread at a time.
 *
 * @param peerName the user on the other side, or `null` to fail to read it.
 * @param reads the counts the reads return, in order; `-1` ends the stream.
 * @param readFailure what a read throws once the reads are used up; a closed channel by default.
 * @param writeChunk the most bytes one write takes.
 * @param writeFailure what writing and shutting the output down throw, or `null` for nothing.
 * @param closeFailure what closing throws, or `null` for nothing.
 * @since 1.0.0
 */
@DavilsInternal
public class FakeUnixSocket(
    private val peerName: String? = FakeIpcEnvironment.USER,
    reads: List<Int> = emptyList(),
    private val readFailure: IOException = ClosedChannelException(),
    private val writeChunk: Int = Int.MAX_VALUE,
    private val writeFailure: IOException? = null,
    private val closeFailure: IOException? = null,
) : UnixSocket {
    private val pendingReads = ArrayDeque(reads)

    /**
     * How many bytes were written.
     *
     * @since 1.0.0
     */
    public var written: Int = 0
        private set

    /**
     * How often the socket was closed.
     *
     * @since 1.0.0
     */
    public var closes: Int = 0
        private set

    /**
     * Whether the output was shut down.
     *
     * @since 1.0.0
     */
    public var isOutputShut: Boolean = false
        private set

    override fun peerUser(): String = peerName ?: throw IOException("no credentials")

    override fun read(buffer: ByteBuffer): Int {
        val count = pendingReads.removeFirstOrNull() ?: throw readFailure
        if (count > 0) buffer.position(buffer.position() + count)
        return count
    }

    override fun write(buffer: ByteBuffer): Int {
        if (writeFailure != null) throw writeFailure
        val count = minOf(a = writeChunk, b = buffer.remaining())
        buffer.position(buffer.position() + count)
        written += count
        return count
    }

    override fun shutdownOutput() {
        if (writeFailure != null) throw writeFailure
        isOutputShut = true
    }

    override fun close() {
        closes += 1
        if (closeFailure != null) throw closeFailure
    }
}
