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

package com.davils.konnect.ipc.client

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.meter.Counter
import com.davils.konfetti.core.publish.Emitter
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry

private const val CONNECTS: String = "konnect.ipc.client.connects"
private const val CONNECTS_TEXT: String = "The attempts of local clients to connect, by how they ended."
private const val OUTCOME: String = "outcome"

internal class ClientTelemetry(telemetry: IpcTelemetry, private val address: IpcAddress) {
    private val connects: Family<Counter>? = telemetry.meters?.let(::connectsOf)

    private val emitter: Emitter<IpcClientEvent>? by lazy {
        telemetry.events?.let { bus -> Emitter(bus, IpcClientTopics.EVENTS) }
    }

    fun connected(result: IpcConnectResult) {
        val counter = connects?.with(result.outcome)
        counter?.increment()
        if (result is IpcConnectResult.ServerRejected) published(result.peer)
    }

    private fun published(peer: IpcPeer): Boolean {
        val target = emitter ?: return false
        return target.send(IpcClientEvent.ServerRejected(address, peer))
    }

    private companion object {
        fun connectsOf(registry: MeterRegistry): Family<Counter> {
            val registration = registry.counter(CONNECTS) {
                description = CONNECTS_TEXT
                labelKeys(OUTCOME)
            }
            return registration.family
        }
    }
}
