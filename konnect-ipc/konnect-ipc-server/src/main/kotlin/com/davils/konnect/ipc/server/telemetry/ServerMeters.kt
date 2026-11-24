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

package com.davils.konnect.ipc.server.telemetry

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.UpDownCounter
import com.davils.konnect.ipc.core.address.IpcAddress

private const val ACTIVE: String = "konnect.ipc.connections.active"
private const val CONNECTIONS: String = "konnect.ipc.connections"
private const val EVENTS_REJECTED: String = "konnect.ipc.events.rejected"
private const val ADDRESS: String = "address"
private const val OUTCOME: String = "outcome"
private const val ACCEPTED: String = "accepted"
private const val REJECTED: String = "rejected"

internal class ServerMeters(registry: MeterRegistry?, address: IpcAddress) {
    private val label: String = address.toString()

    private val active: UpDownCounter? = activeOf(registry)?.with(label)

    private val accepted: Counter? = connectionsOf(registry)?.with(label, ACCEPTED)

    private val rejected: Counter? = connectionsOf(registry)?.with(label, REJECTED)

    private val eventsRejected: Counter? = eventsOf(registry)?.with(label)

    fun opened() {
        active?.increment()
        accepted?.increment()
    }

    fun closed() {
        active?.decrement()
    }

    fun rejected() {
        rejected?.increment()
    }

    fun eventRejected() {
        eventsRejected?.increment()
    }

    private fun activeOf(registry: MeterRegistry?): Family<UpDownCounter>? {
        if (registry == null) return null
        val registration = registry.upDownCounter(ACTIVE) {
            description = "The connections of the server that are open."
            labelKeys(ADDRESS)
        }
        return registration.family
    }

    private fun connectionsOf(registry: MeterRegistry?): Family<Counter>? {
        if (registry == null) return null
        val registration = registry.counter(CONNECTIONS) {
            description = "The connections the server accepted or refused, by outcome."
            labelKeys(ADDRESS, OUTCOME)
        }
        return registration.family
    }

    private fun eventsOf(registry: MeterRegistry?): Family<Counter>? {
        if (registry == null) return null
        val registration = registry.counter(EVENTS_REJECTED) {
            description = "The events of the server the event bus refused to take."
            labelKeys(ADDRESS)
        }
        return registration.family
    }
}
