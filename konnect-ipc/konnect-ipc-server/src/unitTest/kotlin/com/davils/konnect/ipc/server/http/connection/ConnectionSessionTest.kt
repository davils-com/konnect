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

package com.davils.konnect.ipc.server.http.connection

import com.davils.kollect.core.meterRegistry
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.fixture.sumOf
import com.davils.konnect.ipc.server.http.exchange.fixture.get
import com.davils.konnect.ipc.server.http.fixture.ScriptedEndpoint
import com.davils.konnect.ipc.server.http.fixture.served
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val FAILURES: String = "konnect.ipc.http_server.failures"
private const val BUFFER: Int = 1024
private const val POLL: Int = 5

class ConnectionSessionTest : FunSpec({
    context("a connection that fails") {
        test("while it is read is closed and counted as lost") {
            val registry = meterRegistry {}
            val endpoint = ScriptedEndpoint(telemetry = ipcTelemetry { meters(registry) })
            served(endpoint = endpoint) { running ->
                val channel = ScriptedChannel(
                    reads = listOf(IpcChannelRead.Failed(IpcSystemError(code = 5, description = "broken"))),
                )
                running.endpoint.offer(IpcAcceptResult.Accepted(IpcConnection(channel, bufferSize = BUFFER)))
                while (channel.closes == 0) delay(POLL.milliseconds)
                while (running.engine.openConnections > 0) delay(POLL.milliseconds)

                registry.sumOf(FAILURES, "address" to SERVICE.toString(), "reason" to "lost") shouldBe 1.0
            }
        }

        test("while the answer is written is closed and counted as lost") {
            val registry = meterRegistry {}
            val endpoint = ScriptedEndpoint(telemetry = ipcTelemetry { meters(registry) })
            val release = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            served(endpoint = endpoint, routes = {
                get("/slow") {
                    entered.complete(Unit)
                    release.await()
                    call.respondText("too late")
                }
            }) { running ->
                val client = running.connect()
                client.send(get("/slow"))
                entered.await()
                client.close()
                release.complete(Unit)
                while (running.engine.openConnections > 0) delay(POLL.milliseconds)

                registry.sumOf(FAILURES, "address" to SERVICE.toString(), "reason" to "lost") shouldBe 1.0
            }
        }
    }
})
