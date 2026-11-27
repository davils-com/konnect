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

@file:OptIn(DavilsInternal::class, DavilsExperimental::class)

package com.davils.konnect.ipc.server

import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.trace.Span
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.telemetry.traced
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.server.builder.IpcServerConfiguration
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.telemetry.ServerTelemetry
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.IpcListener
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.ipc.server.transport.ListenTransport
import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.AdmissionGate
import com.davils.kore.core.concurrency.Permit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A server of local connections on a Unix domain socket or a Windows named pipe, created by [ipcServer].
 *
 * A server is bound once with [bind] and then hands out one connection per [accept]. Before a connection is handed
 * out, the user of its peer is checked against [IpcServerConfiguration.access]; a peer whose user may not connect or
 * cannot be read is refused, its connection is closed, the refusal is counted and published as
 * [IpcServerEvent.PeerRejected], and [accept] answers [IpcAcceptResult.PeerRejected]. At most
 * [IpcServerConfiguration.maxConnections] connections are open at the same time; an accept waits until a connection
 * is closed.
 *
 * On Unix, the socket is bound in a private staging directory next to its path, restricted there and only then moved
 * to its path in one atomic step, so its path never exists with more rights than configured. The directory of a named
 * address is created, must belong to the current user and is restricted; a socket left behind by a server that is
 * gone is replaced, a socket that answers is not. On Windows, the first instance of the pipe is created as the first
 * instance of its name, with a protected DACL for the allowed users only.
 *
 * The server reports through [IpcServerConfiguration.telemetry] and never logs: the families
 * `konnect.ipc.connections.active`, `konnect.ipc.connections` with the label `outcome` and
 * `konnect.ipc.events.rejected`, each labelled with the `address`, a span `ipc.accept` for every accept, and the
 * events of [IpcServerEvent]. As a [HealthIndicator], it is Up until it is closed.
 *
 * A server is safe to use from several coroutines.
 *
 * @property configuration how the server listens.
 * @since 1.0.0
 */
@OptIn(ExperimentalAtomicApi::class)
public class IpcServer internal constructor(
    public val configuration: IpcServerConfiguration,
    private val transport: ListenTransport,
) : HealthIndicator {
    private val mutableState = MutableStateFlow(IpcServerState.Created)

    private val lifecycle = Mutex()

    private val listener = AtomicReference<IpcListener?>(null)

    private val slots = Semaphore(configuration.maxConnections)

    private val gate = AdmissionGate()

    private val telemetry = ServerTelemetry(configuration.address, configuration.telemetry)

    /**
     * Where the server is in its life.
     *
     * @since 1.0.0
     */
    public val state: StateFlow<IpcServerState> = mutableState.asStateFlow()

    /**
     * How many connections the server handed out that are still open.
     *
     * @since 1.0.0
     */
    public val openConnections: Int get() = telemetry.openConnections

    /**
     * Binds the server to its address, once.
     *
     * @return [IpcBindResult.Bound], [IpcBindResult.AlreadyBound] for a server that listens already,
     *   [IpcBindResult.Closed] for a closed server, or why the address could not be bound.
     * @since 1.0.0
     */
    public suspend fun bind(): IpcBindResult = lifecycle.withLock {
        val current = state.value
        if (current == IpcServerState.Closed) return IpcBindResult.Closed
        if (current == IpcServerState.Listening) return IpcBindResult.AlreadyBound
        listen()
    }

    /**
     * Waits for the next peer, checks it and hands out its connection.
     *
     * The call suspends while every connection is taken and until a peer connects. Cancelling it gives up the wait
     * and keeps the server listening.
     *
     * @return the connection of a peer that may connect, [IpcAcceptResult.PeerRejected] for a peer that may not,
     *   [IpcAcceptResult.NotListening] for a server that is not bound or was closed, or [IpcAcceptResult.Failed].
     * @since 1.0.0
     */
    public suspend fun accept(): IpcAcceptResult {
        val current = listener.load() ?: return IpcAcceptResult.NotListening
        slots.acquire()
        var isHandedOver = false
        try {
            val admitted = gate.tryEnter()?.let { permit -> admittedWith(current, permit) }
            val result = admitted ?: IpcAcceptResult.NotListening
            isHandedOver = result is IpcAcceptResult.Accepted
            return result
        } finally {
            if (!isHandedOver) slots.release()
        }
    }

    /**
     * Stops listening for good and removes the socket of the server; connections that were handed out stay open.
     * Closing it again does nothing.
     *
     * @since 1.0.0
     */
    public fun close() {
        val previous = mutableState.getAndUpdate { IpcServerState.Closed }
        if (previous == IpcServerState.Closed) return
        gate.close()
        listener.exchange(null)?.close()
        telemetry.emit { IpcServerEvent.Closed(configuration.address) }
    }

    /**
     * Suspends until the server is closed and every connection it handed out is closed again, so a shutdown can let
     * the open connections finish after [close]; no connection is handed out once [close] returned.
     *
     * It returns at once for a closed server without open connections and waits for [close] while the server is
     * listening. The server keeps no timer of its own: a caller that cannot wait indefinitely wraps the call in
     * `withTimeoutOrNull`. Cancelling it ends the wait and changes nothing.
     *
     * @since 1.2.0
     */
    public suspend fun awaitIdle() {
        gate.awaitIdle()
    }

    /**
     * Tells whether the server still serves.
     *
     * @return [Health.Up] until the server is closed, then Down.
     * @since 1.0.0
     */
    override suspend fun check(): Health {
        if (state.value != IpcServerState.Closed) return Health.Up
        return Health.down(ReasonCode.Reported)
    }

    /**
     * Shows the address and the state.
     *
     * @return the server as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcServer(${configuration.address}, ${state.value})"

    private suspend fun listen(): IpcBindResult {
        val outcome = transport.listen(configuration.address, configuration.access, configuration.bufferSize)
        return when (outcome) {
            is ListenOutcome.Refused -> outcome.result
            is ListenOutcome.Listening -> listening(outcome.listener)
        }
    }

    private fun listening(bound: IpcListener): IpcBindResult {
        listener.store(bound)
        if (!mutableState.compareAndSet(expect = IpcServerState.Created, update = IpcServerState.Listening)) {
            listener.store(null)
            bound.close()
            return IpcBindResult.Closed
        }
        telemetry.emit { IpcServerEvent.Bound(configuration.address) }
        return IpcBindResult.Bound
    }

    private suspend fun admittedWith(current: IpcListener, permit: Permit): IpcAcceptResult {
        var isHandedOver = false
        try {
            val span = telemetry.startAccept()
            val result = span.traced { admitted(current.accept(), span, permit) }
            isHandedOver = result is IpcAcceptResult.Accepted
            return result
        } finally {
            if (!isHandedOver) permit.close()
        }
    }

    private fun admitted(outcome: AcceptOutcome, span: Span, permit: Permit): IpcAcceptResult = when (outcome) {
        is AcceptOutcome.Refused -> outcome.result
        is AcceptOutcome.Accepted -> checked(outcome.channel, span, permit)
    }

    private fun checked(channel: IpcChannel, span: Span, permit: Permit): IpcAcceptResult {
        val peer = channel.peer
        if (!configuration.access.permits(user = peer.user, currentUser = transport.currentUser)) {
            channel.close()
            telemetry.rejected(peer, span)
            return IpcAcceptResult.PeerRejected(peer)
        }
        val connection = IpcConnection(
            channel = channel,
            bufferSize = configuration.bufferSize,
            onClose = { released(permit) },
        )
        telemetry.opened(peer)
        return IpcAcceptResult.Accepted(connection)
    }

    private fun released(permit: Permit) {
        telemetry.closed()
        permit.close()
        slots.release()
    }
}
