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

import com.davils.konnect.ipc.server.IpcServer
import com.davils.konnect.ipc.server.http.IpcHttpServerConfig
import com.davils.konnect.ipc.server.http.IpcHttpServerEngine
import com.davils.konnect.ipc.server.http.endpoint.IpcEndpoint
import com.davils.kore.core.validation.validateOrThrow
import io.ktor.events.Events
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.engine.ApplicationEngineFactory

private const val SUBJECT: String = "ipc http server"

internal class EngineFactory(private val endpointOf: (IpcServer) -> IpcEndpoint) :
    ApplicationEngineFactory<IpcHttpServerEngine, IpcHttpServerConfig> {
    override fun configuration(configure: IpcHttpServerConfig.() -> Unit): IpcHttpServerConfig {
        val configuration = IpcHttpServerConfig()
        configuration.configure()
        return configuration
    }

    override fun create(
        environment: ApplicationEnvironment,
        monitor: Events,
        developmentMode: Boolean,
        configuration: IpcHttpServerConfig,
        applicationProvider: () -> Application,
    ): IpcHttpServerEngine {
        configuration.validateOrThrow(SUBJECT)
        return IpcHttpServerEngine(
            environment = environment,
            monitor = monitor,
            developmentMode = developmentMode,
            configuration = configuration,
            applicationProvider = applicationProvider,
            endpoint = endpointOf(configuration.boundServer()),
        )
    }
}
