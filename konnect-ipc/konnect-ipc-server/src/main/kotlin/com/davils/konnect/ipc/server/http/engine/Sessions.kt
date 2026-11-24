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

package com.davils.konnect.ipc.server.http.engine

import com.davils.konnect.ipc.server.http.connection.ConnectionSession
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class Sessions {
    private val active: MutableMap<Job, ConnectionSession> = ConcurrentHashMap()

    private val draining = AtomicBoolean(false)

    val size: Int get() = active.size

    fun add(job: Job, session: ConnectionSession) {
        active[job] = session
        if (draining.load()) drained(job, session)
    }

    fun remove(job: Job) {
        active.remove(job)
    }

    fun drain() {
        draining.store(true)
        active.forEach { (job, session) -> drained(job, session) }
    }

    private fun drained(job: Job, session: ConnectionSession) {
        if (session.drain()) job.cancel()
    }
}
