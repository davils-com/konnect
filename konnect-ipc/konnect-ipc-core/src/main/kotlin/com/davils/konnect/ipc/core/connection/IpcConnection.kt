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

package com.davils.konnect.ipc.core.connection

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.framing.IpcFramedConnection
import com.davils.konnect.ipc.core.framing.IpcFraming
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A connection between two local processes, over a Unix domain socket or a Windows named pipe.
 *
 * A connection is created by a client or a server of Konnect, never by its user. It is safe to use from several
 * coroutines: one reader and one writer may work at the same time, while concurrent reads and concurrent writes wait
 * for each other. A read or a write that is cancelled closes the connection, because the bytes it moved are unknown.
 * A connection that failed, or whose other side has gone, closes itself; closing it again does nothing.
 *
 * @param channel the channel that moves the bytes; the connection owns it.
 * @property bufferSize the size of the buffers of the connection, in bytes, from [Limits.MIN_BUFFER_SIZE] to
 *   [Limits.MAX_BUFFER_SIZE]; a read returns at most this many bytes unless it asks for another amount.
 * @param onClose called once, when the connection is closed.
 * @since 1.0.0
 */
@OptIn(ExperimentalAtomicApi::class)
public class IpcConnection @DavilsInternal constructor(
    private val channel: IpcChannel,
    public val bufferSize: Int,
    private val onClose: () -> Unit = {},
) {
    private val open = AtomicBoolean(true)

    private val output = AtomicBoolean(true)

    private val reading = Mutex()

    private val writing = Mutex()

    init {
        require(bufferSize in Limits.MIN_BUFFER_SIZE..Limits.MAX_BUFFER_SIZE) {
            "The buffer of a connection must be from ${Limits.MIN_BUFFER_SIZE} to ${Limits.MAX_BUFFER_SIZE} bytes"
        }
    }

    /**
     * The process on the other side, as the operating system reported it when the connection was made.
     *
     * @since 1.0.0
     */
    public val peer: IpcPeer get() = channel.peer

    /**
     * Whether the connection is still open.
     *
     * @since 1.0.0
     */
    public val isOpen: Boolean get() = open.load()

    /**
     * Whether the connection is open and its output was not closed by [closeOutput].
     *
     * @since 1.0.0
     */
    public val isOutputOpen: Boolean get() = isOpen && output.load()

    /**
     * Reads what the other side sent, at most [maxBytes] bytes, suspending until at least one byte arrived.
     *
     * @param maxBytes the most bytes to read, from 1 to [Limits.MAX_BUFFER_SIZE]; [bufferSize] by default.
     * @return the bytes, [IpcReadResult.Closed] once the other side closed its output or the connection is closed,
     *   or [IpcReadResult.Failed]; the connection is closed after a failure and when the other side has gone.
     * @throws IllegalArgumentException if [maxBytes] is outside of its bounds.
     * @since 1.0.0
     */
    public suspend fun read(maxBytes: Int = bufferSize): IpcReadResult {
        require(maxBytes in 1..Limits.MAX_BUFFER_SIZE) {
            "A read must ask for from 1 to ${Limits.MAX_BUFFER_SIZE} bytes"
        }
        return guarded(reading) { readUpTo(maxBytes) }
    }

    /**
     * Reads exactly [byteCount] bytes, suspending until all of them arrived.
     *
     * The memory for [byteCount] bytes is taken before the first byte is read, so a caller that reads a size the
     * other side announced checks it against its own limit first.
     *
     * @param byteCount how many bytes to read; `0` returns no bytes at once.
     * @return the bytes, [IpcReadResult.Closed] if the other side closed its output before all of them arrived, or
     *   [IpcReadResult.Failed]; a partial read is never returned.
     * @throws IllegalArgumentException if [byteCount] is negative.
     * @since 1.0.0
     */
    public suspend fun readExactly(byteCount: Int): IpcReadResult {
        require(byteCount >= 0) { "The byte count of a read must not be negative" }
        return guarded(reading) { readFully(byteCount) }
    }

    /**
     * Writes every byte of [bytes], suspending until the operating system took all of them.
     *
     * @param bytes the bytes to write.
     * @return [IpcWriteResult.Written], or [IpcWriteResult.Closed] or [IpcWriteResult.Failed], after which the
     *   connection is closed.
     * @since 1.0.0
     */
    public suspend fun write(bytes: ByteString): IpcWriteResult {
        if (!isOutputOpen) return IpcWriteResult.Closed
        val outcome = guardedWrite { channel.write(source = bytes.toByteArray(), offset = 0, length = bytes.size) }
        if (outcome != IpcWriteResult.Written) close()
        return outcome
    }

    /**
     * Closes the output of the connection, so the other side reads its end, while what the other side still sends
     * stays readable. It happens once; every later write is refused.
     *
     * @return [IpcWriteResult.Written], [IpcWriteResult.Closed] if the connection or its output is closed already,
     *   or [IpcWriteResult.Failed], after which the connection is closed.
     * @since 1.0.0
     */
    public suspend fun closeOutput(): IpcWriteResult {
        val isFirst = output.compareAndSet(expectedValue = true, newValue = false)
        if (!isOpen || !isFirst) return IpcWriteResult.Closed
        val outcome = guardedWrite { channel.closeOutput() }
        if (outcome is IpcWriteResult.Failed) close()
        return outcome
    }

    /**
     * Sends and receives frames of [framing] over this connection.
     *
     * @param F the type of a frame.
     * @param framing how bytes are split into frames.
     * @return the framed connection, which shares this connection.
     * @since 1.0.0
     */
    public fun <F> framed(framing: IpcFraming<F>): IpcFramedConnection<F> = IpcFramedConnection(this, framing)

    /**
     * Closes the connection; a pending read or write ends. Closing it again does nothing.
     *
     * @since 1.0.0
     */
    public fun close() {
        if (!open.compareAndSet(expectedValue = true, newValue = false)) return
        channel.close()
        onClose()
    }

    /**
     * Shows the peer and whether the connection is open.
     *
     * @return the connection as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcConnection(peer=$peer, open=$isOpen)"

    private suspend fun readUpTo(maxBytes: Int): IpcReadResult {
        val target = ByteArray(maxBytes)
        return when (val read = channel.read(target = target, offset = 0, length = maxBytes)) {
            is IpcChannelRead.Read -> IpcReadResult.Received(
                ByteString(data = target, startIndex = 0, endIndex = read.count),
            )

            is IpcChannelRead.Ended -> closed()

            is IpcChannelRead.Failed -> failed(read)
        }
    }

    private suspend fun readFully(byteCount: Int): IpcReadResult {
        val target = ByteArray(byteCount)
        return when (val last = filled(target)) {
            is IpcChannelRead.Read -> IpcReadResult.Received(ByteString(target))
            is IpcChannelRead.Ended -> closed()
            is IpcChannelRead.Failed -> failed(last)
        }
    }

    private suspend fun filled(target: ByteArray): IpcChannelRead {
        var count = 0
        var last: IpcChannelRead = IpcChannelRead.Read(0)
        while (count < target.size && last is IpcChannelRead.Read) {
            last = channel.read(target = target, offset = count, length = target.size - count)
            count += (last as? IpcChannelRead.Read)?.count ?: 0
        }
        return last
    }

    private fun closed(): IpcReadResult {
        close()
        return IpcReadResult.Closed
    }

    private fun failed(read: IpcChannelRead.Failed): IpcReadResult {
        close()
        return IpcReadResult.Failed(read.error)
    }

    private suspend fun guarded(lock: Mutex, block: suspend () -> IpcReadResult): IpcReadResult {
        if (!isOpen) return IpcReadResult.Closed
        return lock.withLock { closingOnCancel(block) }
    }

    private suspend fun guardedWrite(block: suspend () -> IpcWriteResult): IpcWriteResult =
        writing.withLock { closingOnCancel(block) }

    private suspend fun <T> closingOnCancel(block: suspend () -> T): T {
        var isFinished = false
        try {
            return block().also { isFinished = true }
        } finally {
            closeUnless(isFinished)
        }
    }

    private fun closeUnless(isFinished: Boolean) {
        if (!isFinished) close()
    }
}
