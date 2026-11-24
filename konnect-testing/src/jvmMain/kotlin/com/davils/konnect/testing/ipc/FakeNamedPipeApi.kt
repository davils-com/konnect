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

import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap
import com.davils.kore.core.platform.NativeResult
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val FIRST_HANDLE: Long = 1_000L
private const val PATIENCE_SECONDS: Long = 5
private const val INSUFFICIENT_BUFFER: Long = 122

/**
 * Named pipes in memory that answer with the results they were given, for tests of the pipe transports on any
 * operating system.
 *
 * A call takes the next result queued for it and falls back to success otherwise: a new handle for a create or an
 * open, `0` for a wait, the whole length for a write. A read takes the next queued message and fails with
 * [readFailure] once there is none. With a [blocker], a read without a message and a wait for a client without a
 * queued result block until [cancel] is called, at most five seconds, and then fail with
 * [Win32Error.OPERATION_ABORTED]. A security identifier that does not fit the buffer fails as the native library
 * does. It is safe to use from several threads.
 *
 * @param sids the security identifier of each process; [CURRENT_PROCESS] and [PEER_PROCESS] by default.
 * @param blocker the latch that blocking calls wait for, or `null` for calls that never block.
 * @since 1.0.0
 */
@DavilsInternal
@OptIn(ExperimentalAtomicApi::class)
public class FakeNamedPipeApi(
    sids: Map<Long, String> = mapOf(CURRENT_PROCESS to CURRENT_SID, PEER_PROCESS to PEER_SID),
    private val blocker: CountDownLatch? = null,
) : NamedPipeApi {
    private val sids: Map<Long, String> = sids.immutableMap()

    private val createdPipes: MutableList<FakeCreatedPipe> = CopyOnWriteArrayList()

    private val createResults = ConcurrentLinkedQueue<Long>()

    private val awaitResults = ConcurrentLinkedQueue<Long>()

    private val openResults = ConcurrentLinkedQueue<Long>()

    private val waitResults = ConcurrentLinkedQueue<Long>()

    private val inbound = ConcurrentLinkedQueue<ByteArray>()

    private val writeResults = ConcurrentLinkedQueue<Long>()

    private val writtenPieces: MutableList<ByteArray> = CopyOnWriteArrayList()

    private val cancelledHandles: MutableList<Long> = CopyOnWriteArrayList()

    private val closedHandles: MutableList<Long> = CopyOnWriteArrayList()

    private val emptyWriteCount = AtomicInt(0)

    private val nextHandle = AtomicLong(FIRST_HANDLE)

    /**
     * The encoded result of a read without a queued message; a broken pipe unless it is set.
     *
     * @since 1.0.0
     */
    @Volatile
    public var readFailure: Long = -Win32Error.BROKEN_PIPE

    /**
     * The encoded process identifier the other side of every pipe reports; [PEER_PROCESS] unless it is set.
     *
     * @since 1.0.0
     */
    @Volatile
    public var peerProcessId: Long = PEER_PROCESS

    /**
     * The server pipes that were created, in order.
     *
     * @since 1.0.0
     */
    public val created: List<FakeCreatedPipe> get() = createdPipes.immutableList()

    /**
     * The bytes of every write that succeeded, in order.
     *
     * @since 1.0.0
     */
    public val written: List<ByteArray> get() = writtenPieces.immutableList()

    /**
     * The handles that were cancelled, in order.
     *
     * @since 1.0.0
     */
    public val cancelled: List<Long> get() = cancelledHandles.immutableList()

    /**
     * The handles that were closed, in order.
     *
     * @since 1.0.0
     */
    public val closed: List<Long> get() = closedHandles.immutableList()

    /**
     * How many writes of no bytes, which close the output of a pipe, there were.
     *
     * @since 1.0.0
     */
    public val emptyWrites: Int get() = emptyWriteCount.load()

    /**
     * Queues the encoded result of the next create.
     *
     * @param result the encoded result.
     * @since 1.0.0
     */
    public fun queueCreate(result: Long) {
        createResults += result
    }

    /**
     * Queues the encoded result of the next wait for a client.
     *
     * @param result the encoded result.
     * @since 1.0.0
     */
    public fun queueAwait(result: Long) {
        awaitResults += result
    }

    /**
     * Queues the encoded result of the next open.
     *
     * @param result the encoded result.
     * @since 1.0.0
     */
    public fun queueOpen(result: Long) {
        openResults += result
    }

    /**
     * Queues the encoded result of the next wait for a free instance.
     *
     * @param result the encoded result.
     * @since 1.0.0
     */
    public fun queueWait(result: Long) {
        waitResults += result
    }

    /**
     * Queues a message the next read returns.
     *
     * @param bytes the message.
     * @since 1.0.0
     */
    public fun queueInbound(bytes: ByteArray) {
        inbound += bytes.copyOf()
    }

    /**
     * Queues the encoded result of the next write.
     *
     * @param result the encoded result; a positive count writes that many bytes.
     * @since 1.0.0
     */
    public fun queueWrite(result: Long) {
        writeResults += result
    }

    override fun createServer(
        path: String,
        securityDescriptor: String,
        bufferSize: Int,
        isFirstInstance: Boolean,
    ): NativeResult {
        createdPipes += FakeCreatedPipe(path, securityDescriptor, isFirstInstance)
        val result = createResults.poll() ?: nextHandle.fetchAndAdd(1L)
        return NativeResult.fromEncoded(result)
    }

    override fun awaitClient(handle: Long): NativeResult {
        val result = awaitResults.poll() ?: blocked()
        return NativeResult.fromEncoded(result)
    }

    override fun open(path: String): NativeResult {
        val result = openResults.poll() ?: nextHandle.fetchAndAdd(1L)
        return NativeResult.fromEncoded(result)
    }

    override fun waitAvailable(path: String, timeoutMillis: Int): NativeResult {
        val result = waitResults.poll() ?: 0L
        return NativeResult.fromEncoded(result)
    }

    override fun read(handle: Long, buffer: ByteBuffer, length: Int): NativeResult {
        val next = inbound.poll() ?: return NativeResult.fromEncoded(failedRead())
        buffer.put(0, next)
        return NativeResult.success(next.size.toLong())
    }

    override fun write(handle: Long, buffer: ByteBuffer, length: Int): NativeResult {
        if (length == 0) emptyWriteCount.addAndFetch(1)
        val result = writeResults.poll() ?: length.toLong()
        if (result <= 0) return NativeResult.fromEncoded(result)
        val bytes = ByteArray(result.toInt())
        buffer.get(0, bytes)
        writtenPieces += bytes
        return NativeResult.success(result)
    }

    override fun cancel(handle: Long): NativeResult {
        cancelledHandles += handle
        blocker?.countDown()
        return NativeResult.success(0L)
    }

    override fun close(handle: Long): NativeResult {
        closedHandles += handle
        return NativeResult.success(0L)
    }

    override fun clientProcessId(handle: Long): NativeResult = NativeResult.fromEncoded(peerProcessId)

    override fun serverProcessId(handle: Long): NativeResult = NativeResult.fromEncoded(peerProcessId)

    override fun processUserSid(processId: Long, buffer: ByteBuffer): NativeResult {
        val sid = sids[processId]
        val bytes = sid?.toByteArray(Charsets.UTF_16LE)
        if (sid == null || bytes == null) return NativeResult.failure(Win32Error.ACCESS_DENIED)
        return written(sid, bytes, buffer)
    }

    private fun written(sid: String, bytes: ByteArray, buffer: ByteBuffer): NativeResult {
        if (bytes.size > buffer.capacity()) return NativeResult.failure(INSUFFICIENT_BUFFER)
        buffer.put(0, bytes)
        return NativeResult.success(sid.length.toLong())
    }

    private fun failedRead(): Long {
        if (blocker == null) return readFailure
        return blocked()
    }

    private fun blocked(): Long {
        blocker?.await(PATIENCE_SECONDS, TimeUnit.SECONDS)
        return -Win32Error.OPERATION_ABORTED
    }

    /**
     * The processes and users of a fake.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The process of the test, `100`.
         *
         * @since 1.0.0
         */
        public const val CURRENT_PROCESS: Long = 100L

        /**
         * The process on the other side of every pipe, `200`.
         *
         * @since 1.0.0
         */
        public const val PEER_PROCESS: Long = 200L

        /**
         * The user of [CURRENT_PROCESS].
         *
         * @since 1.0.0
         */
        public const val CURRENT_SID: String = "S-1-5-21-1-2-3-1001"

        /**
         * The user of [PEER_PROCESS].
         *
         * @since 1.0.0
         */
        public const val PEER_SID: String = "S-1-5-21-1-2-3-1002"
    }
}
