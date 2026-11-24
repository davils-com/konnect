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

package com.davils.konnect.ipc.server.http.fixture

import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.http.IpcHttpServerConfig
import com.davils.konnect.ipc.server.http.engine.EngineFactory
import com.davils.konnect.ipc.server.ipcServer
import io.ktor.server.application.Application
import io.ktor.server.application.ServerConfig
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val PATIENCE_SECONDS: Int = 10

internal suspend fun <T> served(
    endpoint: ScriptedEndpoint = ScriptedEndpoint(),
    configure: IpcHttpServerConfig.() -> Unit = {},
    routes: Routing.() -> Unit = {},
    isStarted: Boolean = true,
    patience: Duration = PATIENCE_SECONDS.seconds,
    block: suspend (Served) -> T,
): T = withThreads { threads ->
    val server = embeddedServer(EngineFactory { endpoint }, configOf(threads) { routing(routes) }) {
        server = ipcServer(threads) { address = SERVICE }
        configure()
    }
    if (isStarted) server.start(wait = false)
    val scope = CoroutineScope(SupervisorJob() + threads)
    try {
        withTimeout(patience) { block(Served(server, endpoint, scope)) }
    } finally {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 0)
        scope.cancel()
    }
}

internal fun configOf(threads: ExecutorCoroutineDispatcher, module: Application.() -> Unit): ServerConfig =
    serverConfig {
        parentCoroutineContext = threads
        developmentMode = false
        watchPaths = emptyList()
        module(module)
    }
