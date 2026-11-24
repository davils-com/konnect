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

import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocket
import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val PATIENCE_SECONDS: Long = 5

/**
 * A server socket that hands out the sockets it was given.
 *
 * An accept takes the next of [sockets]; once they are used up, it waits until [wakeup] is called, at most five
 * seconds, and returns `null`. It is safe to use from several threads.
 *
 * @param sockets the sockets to hand out, in order; a `null` is an accept that was woken up without a connection.
 * @param failure what every accept throws, or `null` for nothing.
 * @param closeFailure what closing throws; a closed socket by default, as a server socket that was closed twice.
 * @since 1.0.0
 */
@DavilsInternal
@OptIn(ExperimentalAtomicApi::class)
public class FakeUnixServerSocket(
    sockets: List<UnixSocket?> = emptyList(),
    private val failure: IOException? = null,
    private val closeFailure: IOException? = IOException("already closed"),
) : UnixServerSocket {
    private val pending = ConcurrentLinkedQueue(sockets.map { socket -> Accepted(socket) })

    private val woken = CountDownLatch(1)

    private val wakeupCount = AtomicInt(0)

    private val closeCount = AtomicInt(0)

    /**
     * How often [wakeup] was called.
     *
     * @since 1.0.0
     */
    public val wakeups: Int get() = wakeupCount.load()

    /**
     * How often the socket was closed.
     *
     * @since 1.0.0
     */
    public val closes: Int get() = closeCount.load()

    override fun accept(): UnixSocket? {
        if (failure != null) throw failure
        val next = pending.poll()
        if (next != null) return next.socket
        woken.await(PATIENCE_SECONDS, TimeUnit.SECONDS)
        return null
    }

    override fun wakeup() {
        wakeupCount.addAndFetch(1)
        woken.countDown()
    }

    override fun close() {
        closeCount.addAndFetch(1)
        if (closeFailure != null) throw closeFailure
    }

    private class Accepted(val socket: UnixSocket?)
}
