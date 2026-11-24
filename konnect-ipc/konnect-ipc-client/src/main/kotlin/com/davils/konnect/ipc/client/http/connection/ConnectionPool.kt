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

import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.TimeSource

internal class ConnectionPool(
    private val maxIdle: Int,
    maxConnections: Int,
    private val idleTimeout: Duration,
    private val timeSource: TimeSource,
) {
    private val idle: ConcurrentLinkedDeque<IdleConnection> = ConcurrentLinkedDeque()

    private val permits: Semaphore = Semaphore(maxConnections)

    private val closed = AtomicBoolean(false)

    val idleCount: Int get() = idle.size

    val freePermits: Int get() = permits.availablePermits

    suspend fun lease(open: suspend () -> HttpConnection): HttpConnection {
        permits.acquire()
        var leased: HttpConnection? = null
        try {
            val connection = take() ?: open()
            connection.lease()
            leased = connection
            return connection
        } finally {
            releaseUnless(leased)
        }
    }

    fun release(connection: HttpConnection, isReusable: Boolean) {
        if (!connection.endLease()) return
        kept(connection, isReusable)
        permits.release()
    }

    fun closeAll() {
        closed.store(true)
        drained()
    }

    private fun releaseUnless(leased: HttpConnection?) {
        if (leased != null) return
        permits.release()
    }

    private fun drained() {
        if (!closed.load()) return
        var candidate = idle.pollFirst()
        while (candidate != null) {
            candidate.connection.close()
            candidate = idle.pollFirst()
        }
    }

    private fun kept(connection: HttpConnection, isReusable: Boolean) {
        val hasRoom = idle.size < maxIdle && !closed.load()
        val isKept = isReusable && hasRoom && connection.isOpen && connection.isQuiet
        if (!isKept) {
            connection.close()
            return
        }
        idle.addLast(IdleConnection(connection, timeSource.markNow()))
        drained()
    }

    private fun take(): HttpConnection? {
        var candidate = idle.pollFirst()
        while (candidate != null && !candidate.isUsable(idleTimeout)) {
            candidate.connection.close()
            candidate = idle.pollFirst()
        }
        return candidate?.connection
    }
}
