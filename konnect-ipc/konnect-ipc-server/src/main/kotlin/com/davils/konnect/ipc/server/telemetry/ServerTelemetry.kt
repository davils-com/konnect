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

package com.davils.konnect.ipc.server.telemetry

import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanKind
import com.davils.konfetti.core.publish.Emitter
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.IpcServerEvent
import com.davils.konnect.ipc.server.IpcServerTopics
import com.davils.kore.core.annotation.DavilsInternal
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val ACCEPT_OPERATION: String = "ipc.accept"
private const val ADDRESS_ATTRIBUTE: String = "ipc.address"
private const val PEER_ATTRIBUTE: String = "ipc.peer"

@OptIn(ExperimentalAtomicApi::class)
internal class ServerTelemetry(private val address: IpcAddress, private val telemetry: IpcTelemetry) {
    private val meters: ServerMeters = ServerMeters(telemetry.meters, address)

    private val active = AtomicInt(0)

    private val emitter: Emitter<IpcServerEvent>? = telemetry.events?.let { bus ->
        Emitter(
            bus,
            IpcServerTopics.EVENTS,
        )
    }

    val openConnections: Int get() = active.load()

    fun startAccept(): Span {
        val span = telemetry.tracer.start(ACCEPT_OPERATION, SpanKind.Server)
        span.attribute(key = ADDRESS_ATTRIBUTE, value = address.toString())
        return span
    }

    fun opened(peer: IpcPeer) {
        active.addAndFetch(1)
        meters.opened()
        emit { IpcServerEvent.Accepted(address, peer) }
    }

    fun closed() {
        active.addAndFetch(-1)
        meters.closed()
    }

    fun rejected(peer: IpcPeer, span: Span) {
        meters.rejected()
        span.attribute(key = PEER_ATTRIBUTE, value = peer.toString())
        emit { IpcServerEvent.PeerRejected(address, peer) }
    }

    fun emit(event: () -> IpcServerEvent) {
        val target = emitter ?: return
        val isDropped = target.isBound && !target.hasConsumers
        if (isDropped) return
        if (!target.emit(event)) meters.eventRejected()
    }
}
