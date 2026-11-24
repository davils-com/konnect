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

import com.davils.konnect.ipc.server.http.endpoint.ServerEndpoint
import com.davils.konnect.ipc.server.http.engine.EngineFactory
import io.ktor.events.Events
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.engine.ApplicationEngineFactory

/**
 * The Ktor server engine that serves HTTP/1.1 over a local connection, used as
 * `embeddedServer(IpcHttpServer, config) { server = ipcServer(dispatcher) { address = myAddress } }`.
 *
 * Every connection the IPC server accepted, after it checked the peer, carries HTTP/1.1 with persistent connections,
 * pipelining, `100-continue`, chunked bodies in both directions and protocol switches. Every call names its peer and
 * its address through [IpcHttpCallAttributes]. The factory holds no state.
 *
 * @since 1.0.0
 */
public object IpcHttpServer : ApplicationEngineFactory<IpcHttpServerEngine, IpcHttpServerConfig> {
    private val factory: EngineFactory = EngineFactory(::ServerEndpoint)

    /**
     * Creates the configuration of an engine and runs [configure] on it.
     *
     * @param configure the block that sets the configuration.
     * @return the configuration, not validated yet.
     * @since 1.0.0
     */
    override fun configuration(configure: IpcHttpServerConfig.() -> Unit): IpcHttpServerConfig =
        factory.configuration(configure)

    /**
     * Creates an engine that serves [applicationProvider] on the IPC server of [configuration].
     *
     * @param environment the environment of the application.
     * @param monitor the events of the application.
     * @param developmentMode whether the application runs in development mode.
     * @param configuration the configuration of the engine.
     * @param applicationProvider provides the application.
     * @return the engine, not started yet.
     * @throws com.davils.kore.core.validation.report.ValidationException if [configuration] is invalid; the report
     *   holds every violation with the codes of [IpcHttpServerViolationCodes].
     * @since 1.0.0
     */
    override fun create(
        environment: ApplicationEnvironment,
        monitor: Events,
        developmentMode: Boolean,
        configuration: IpcHttpServerConfig,
        applicationProvider: () -> Application,
    ): IpcHttpServerEngine = factory.create(
        environment = environment,
        monitor = monitor,
        developmentMode = developmentMode,
        configuration = configuration,
        applicationProvider = applicationProvider,
    )
}
