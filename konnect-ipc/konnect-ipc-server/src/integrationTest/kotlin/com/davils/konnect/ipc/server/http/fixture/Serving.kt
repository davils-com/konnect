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

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.http.IpcHttpServer
import com.davils.konnect.ipc.server.ipcServer
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private const val PATIENCE_SECONDS: Int = 20
private const val STOP_MILLIS: Long = 1_000

internal suspend fun <T> served(
    address: IpcAddress,
    routes: Routing.() -> Unit,
    block: suspend (ExecutorCoroutineDispatcher) -> T,
): T = withThreads { threads ->
    val config = serverConfig {
        parentCoroutineContext = threads
        developmentMode = false
        watchPaths = emptyList()
        module { routing(routes) }
    }
    val server = embeddedServer(IpcHttpServer, config) { server = ipcServer(threads) { this.address = address } }
    server.start(wait = false)
    try {
        withTimeout(PATIENCE_SECONDS.seconds) { block(threads) }
    } finally {
        server.stop(gracePeriodMillis = 0, timeoutMillis = STOP_MILLIS)
    }
}
