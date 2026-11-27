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

package com.davils.konnect.ipc.shutdown

import com.davils.knot.shutdown.ShutdownPlanBuilder
import com.davils.konnect.ipc.server.IpcServer
import com.davils.konnect.ipc.server.http.IpcHttpServerEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_SERVER_NAME: String = "ipc-server"
private const val DEFAULT_ENGINE_NAME: String = "ipc-http-server"
private val stopMargin: Duration = 1.seconds

/**
 * Closes [server] in the stop phase of the shutdown, in a task named [name]: the server stops listening and removes its
 * socket, and the task waits through [IpcServer.awaitIdle] until the connections it handed out are closed by their
 * owners. A connection that outlives the timeout of the task stays open and the report of the shutdown names the task
 * as timed out, so a connection that never ends is visible.
 *
 * Closing a server that is closed already does nothing, so the task never fails because the application closed the
 * server first.
 *
 * @param server the server.
 * @param name the name of the task, `ipc-server` by default; it must be unique in the shutdown.
 * @since 1.1.0
 */
public fun ShutdownPlanBuilder<*>.ipcServer(server: IpcServer, name: String = DEFAULT_SERVER_NAME) {
    stop {
        task(name) {
            action {
                server.close()
                server.awaitIdle()
            }
        }
    }
}

/**
 * Stops [engine] in the drain phase of the shutdown, in a task named [name]: the engine stops accepting, closes its IPC
 * server and idle connections, lets the calls in flight finish within [gracePeriod] and cancels the rest.
 *
 * The task may take [gracePeriod] and one more second to cancel what is left; both are cut to the time the shutdown
 * still has. Stopping an engine that is not running does nothing.
 *
 * @param engine the engine.
 * @param gracePeriod how long the calls in flight may take to finish, 5 seconds by default.
 * @param name the name of the task, `ipc-http-server` by default; it must be unique in the shutdown.
 * @throws IllegalArgumentException if [gracePeriod] is negative or not finite.
 * @since 1.1.0
 */
public fun ShutdownPlanBuilder<*>.ipcHttpServer(
    engine: IpcHttpServerEngine,
    gracePeriod: Duration = 5.seconds,
    name: String = DEFAULT_ENGINE_NAME,
) {
    require(!gracePeriod.isNegative() && gracePeriod.isFinite()) {
        "The grace period of an engine must be finite and not negative, but was $gracePeriod"
    }
    drain {
        task(name) {
            timeout = gracePeriod + stopMargin
            action {
                val total = remaining.coerceAtLeast(Duration.ZERO)
                val grace = minOf(a = gracePeriod, b = (total - stopMargin).coerceAtLeast(Duration.ZERO))
                engine.stopSuspend(
                    gracePeriodMillis = grace.inWholeMilliseconds,
                    timeoutMillis = total.inWholeMilliseconds,
                )
            }
        }
    }
}
