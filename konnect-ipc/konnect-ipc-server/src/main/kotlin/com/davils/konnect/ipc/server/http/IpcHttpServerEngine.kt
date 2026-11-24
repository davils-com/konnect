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

package com.davils.konnect.ipc.server.http

import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.server.http.connection.ConnectionSession
import com.davils.konnect.ipc.server.http.connection.SessionContext
import com.davils.konnect.ipc.server.http.endpoint.IpcEndpoint
import com.davils.konnect.ipc.server.http.engine.EngineState
import com.davils.konnect.ipc.server.http.engine.EngineTelemetry
import com.davils.konnect.ipc.server.http.engine.IpcConnector
import com.davils.konnect.ipc.server.http.engine.Sessions
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import io.ktor.events.Events
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ServerReady
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.BaseApplicationEngine
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

private const val MAX_ACCEPT_FAILURES: Int = 16

/**
 * The engine [IpcHttpServer] creates: it binds its IPC server when it starts, serves every connection it accepts and
 * closes the server when it stops.
 *
 * Stopping lets the calls in flight finish within the grace period, closes idle connections at once and every
 * connection once its call is answered, and cancels what is still running once the grace period is over. After 16
 * failed accepts in a row the engine stops accepting and reports Down.
 *
 * The engine reports through the telemetry of its IPC server and never logs: the families
 * `konnect.ipc.http_server.connections.active`, `konnect.ipc.http_server.connections`,
 * `konnect.ipc.http_server.requests` and `konnect.ipc.http_server.failures` with the label `reason`, each labelled
 * with the `address`. As a [HealthIndicator], it is Up while it runs and accepts. It is safe to use from several
 * threads.
 *
 * @property configuration the configuration of the engine.
 * @since 1.0.0
 */
@OptIn(ExperimentalAtomicApi::class)
public class IpcHttpServerEngine internal constructor(
    environment: ApplicationEnvironment,
    monitor: Events,
    developmentMode: Boolean,
    public val configuration: IpcHttpServerConfig,
    applicationProvider: () -> Application,
    private val endpoint: IpcEndpoint,
) : BaseApplicationEngine(environment, monitor, developmentMode),
    HealthIndicator {
    private val telemetry: EngineTelemetry = EngineTelemetry(endpoint.telemetry.meters, endpoint.address)

    private val state = AtomicReference(EngineState.Created)

    private val accepting = AtomicBoolean(true)

    private val sessions: Sessions = Sessions()

    private val parent: CoroutineContext = applicationProvider().parentCoroutineContext

    private val serverJob: CompletableJob = SupervisorJob(parent[Job])

    private val scope: CoroutineScope = CoroutineScope(parent + serverJob)

    private val context: SessionContext = SessionContext(
        idleTimeout = configuration.idleTimeout,
        requestBodyTimeout = configuration.requestBodyTimeout,
        maxHeaderSize = configuration.maxHeaderSize,
        maxRequestBodySize = configuration.maxRequestBodySize,
        maxRequestsPerConnection = configuration.maxRequestsPerConnection,
        telemetry = telemetry,
        address = endpoint.address,
        pipeline = pipeline,
        application = applicationProvider,
    )

    private val acceptor: Job = scope.launch(start = CoroutineStart.LAZY) { acceptLoop() }

    private val stopped: CompletableJob = Job()

    /**
     * How many connections the engine serves now.
     *
     * @since 1.0.0
     */
    public val openConnections: Int get() = telemetry.openConnections

    /**
     * Starts the engine, blocking the calling thread while its IPC server binds.
     *
     * @param wait whether to block until the engine stops.
     * @return this engine.
     * @throws IllegalStateException if the engine was started before.
     * @throws IpcHttpServerBindException if the IPC server cannot listen; the engine is stopped then.
     * @since 1.0.0
     */
    override fun start(wait: Boolean): ApplicationEngine = runBlocking { startSuspend(wait) }

    /**
     * Starts the engine: binds its IPC server, names it as the only connector and starts accepting.
     *
     * @param wait whether to suspend until the engine stops.
     * @return this engine.
     * @throws IllegalStateException if the engine was started before.
     * @throws IpcHttpServerBindException if the IPC server cannot listen; the engine is stopped then.
     * @since 1.0.0
     */
    override suspend fun startSuspend(wait: Boolean): ApplicationEngine {
        val isFirst = state.compareAndSet(expectedValue = EngineState.Created, newValue = EngineState.Running)
        check(isFirst) { "The engine was already started" }
        bound()
        resolvedConnectorsDeferred.complete(listOf(IpcConnector(endpoint.address)))
        acceptor.start()
        monitor.raise(ServerReady, environment)
        if (wait) stopped.join()
        return this
    }

    /**
     * Stops the engine, blocking the calling thread until it stopped.
     *
     * @param gracePeriodMillis how long calls in flight may take to finish.
     * @param timeoutMillis how long stopping may take at most, the grace period included.
     * @since 1.0.0
     */
    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        runBlocking { stopSuspend(gracePeriodMillis, timeoutMillis) }
    }

    /**
     * Stops the engine: stops accepting, closes the IPC server and its idle connections, lets the calls in flight
     * finish within [gracePeriodMillis] and cancels the rest. Stopping an engine that is not running does nothing.
     *
     * @param gracePeriodMillis how long calls in flight may take to finish.
     * @param timeoutMillis how long stopping may take at most, the grace period included.
     * @since 1.0.0
     */
    override suspend fun stopSuspend(gracePeriodMillis: Long, timeoutMillis: Long) {
        if (!state.compareAndSet(expectedValue = EngineState.Running, newValue = EngineState.Stopping)) return
        monitor.raise(ApplicationStopPreparing, environment)
        acceptor.cancel()
        endpoint.close()
        sessions.drain()
        if (!settled(gracePeriodMillis)) forced(timeoutMillis - gracePeriodMillis)
        serverJob.cancel()
        state.store(EngineState.Stopped)
        stopped.complete()
    }

    /**
     * Tells whether the engine serves.
     *
     * @return [Health.Up] while the engine runs and accepts connections, Down before it started, once it stopped
     *   accepting after repeated failures and once it stopped.
     * @since 1.0.0
     */
    override suspend fun check(): Health {
        val isServing = state.load() == EngineState.Running && accepting.load()
        if (isServing) return Health.Up
        return Health.down(ReasonCode.Reported)
    }

    /**
     * Shows the address and the state.
     *
     * @return the engine as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcHttpServerEngine(${endpoint.address}, ${state.load()})"

    private suspend fun bound() {
        val result = endpoint.bind()
        if (result == IpcBindResult.Bound || result == IpcBindResult.AlreadyBound) return
        state.store(EngineState.Stopped)
        stopped.complete()
        serverJob.cancel()
        throw IpcHttpServerBindException(result)
    }

    private suspend fun acceptLoop() {
        var failures = 0
        var isListening = true
        while (isListening && failures < MAX_ACCEPT_FAILURES) {
            val result = endpoint.accept()
            isListening = result != IpcAcceptResult.NotListening
            failures = failuresAfter(result, failures)
        }
        if (isListening) accepting.store(false)
    }

    private fun failuresAfter(result: IpcAcceptResult, failures: Int): Int = when (result) {
        is IpcAcceptResult.Accepted -> accepted(result.connection)
        is IpcAcceptResult.PeerRejected -> failures
        is IpcAcceptResult.Failed -> failed(failures)
        IpcAcceptResult.NotListening -> failures
    }

    private fun accepted(connection: IpcConnection): Int {
        opened(connection)
        return 0
    }

    private fun failed(failures: Int): Int {
        telemetry.acceptFailed()
        return failures + 1
    }

    private fun opened(connection: IpcConnection) {
        val session = ConnectionSession(connection, context)
        val job = scope.launch(start = CoroutineStart.LAZY) { session.serve() }
        job.invokeOnCompletion {
            sessions.remove(job)
            connection.close()
            telemetry.closed()
        }
        sessions.add(job, session)
        telemetry.opened()
        job.start()
    }

    private suspend fun settled(limit: Long): Boolean = withTimeoutOrNull(limit) { joinedChildren() } != null

    private suspend fun forced(limit: Long) {
        serverJob.cancelChildren()
        withTimeoutOrNull(limit) { joinedChildren() }
    }

    private suspend fun joinedChildren() {
        for (child in serverJob.children) child.join()
    }
}
