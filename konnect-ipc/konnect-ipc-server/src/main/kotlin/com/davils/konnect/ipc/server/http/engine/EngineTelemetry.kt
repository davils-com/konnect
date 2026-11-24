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

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.UpDownCounter
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.server.http.exchange.Rejection
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val ACTIVE: String = "konnect.ipc.http_server.connections.active"
private const val CONNECTIONS: String = "konnect.ipc.http_server.connections"
private const val REQUESTS: String = "konnect.ipc.http_server.requests"
private const val FAILURES: String = "konnect.ipc.http_server.failures"
private const val ADDRESS: String = "address"
private const val REASON: String = "reason"
private const val LOST: String = "lost"
private const val ACCEPT: String = "accept"

@OptIn(ExperimentalAtomicApi::class)
internal class EngineTelemetry(registry: MeterRegistry?, address: IpcAddress) {
    private val label: String = address.toString()

    private val active = AtomicInt(0)

    private val activeMeter: UpDownCounter? = activeOf(registry)?.with(label)

    private val connections: Counter? = counterOf(
        registry = registry,
        name = CONNECTIONS,
        help = "The connections the engine served.",
    )

    private val requests: Counter? = counterOf(
        registry = registry,
        name = REQUESTS,
        help = "The requests the engine handed to the application.",
    )

    private val failures: Family<Counter>? = failuresOf(registry)

    val openConnections: Int get() = active.load()

    fun opened() {
        active.addAndFetch(1)
        activeMeter?.increment()
        connections?.increment()
    }

    fun closed() {
        active.addAndFetch(-1)
        activeMeter?.decrement()
    }

    fun served() {
        requests?.increment()
    }

    fun rejected(rejection: Rejection) {
        failed(rejection.reason)
    }

    fun lost() {
        failed(LOST)
    }

    fun acceptFailed() {
        failed(ACCEPT)
    }

    private fun failed(reason: String) {
        val meter = failures?.with(label, reason) ?: return
        meter.increment()
    }

    private fun activeOf(registry: MeterRegistry?): Family<UpDownCounter>? {
        if (registry == null) return null
        val registration = registry.upDownCounter(ACTIVE) {
            description = "The connections of the engine that are open."
            labelKeys(ADDRESS)
        }
        return registration.family
    }

    private fun counterOf(registry: MeterRegistry?, name: String, help: String): Counter? {
        if (registry == null) return null
        val registration = registry.counter(name) {
            description = help
            labelKeys(ADDRESS)
        }
        return registration.family.with(label)
    }

    private fun failuresOf(registry: MeterRegistry?): Family<Counter>? {
        if (registry == null) return null
        val registration = registry.counter(FAILURES) {
            description = "The requests and connections the engine could not serve, by reason."
            labelKeys(ADDRESS, REASON)
        }
        return registration.family
    }
}
