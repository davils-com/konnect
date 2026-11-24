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
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.meterRegistry
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.fixture.sumOf
import com.davils.konnect.ipc.server.http.exchange.fixture.get
import com.davils.konnect.ipc.server.http.fixture.ScriptedEndpoint
import com.davils.konnect.ipc.server.http.fixture.served
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val HELLO: String = "/hello"
private const val FAILURES: Int = 16
private const val GREETING: String = "hello"
private const val POLL: Int = 5
private const val PAUSE: Int = 20
private const val GRACE: Long = 5_000
private const val TIMEOUT: Long = 6_000
private val address: Pair<String, String> = "address" to SERVICE.toString()

class IpcHttpServerEngineTest : FunSpec({
    context("starting") {
        test("binds the endpoint, names it as the connector and is healthy") {
            served { running ->
                val connector = running.engine.resolvedConnectors().single()

                connector.addressDescription shouldBe SERVICE.toString()
                connector.port shouldBe 0
                connector.host shouldBe "localhost"
                connector.type.name shouldBe "IPC"
                running.engine.check() shouldBe Health.Up
                running.engine.toString() shouldContain "Running"
            }
        }

        test("takes an endpoint that already listens") {
            served(endpoint = ScriptedEndpoint(IpcBindResult.AlreadyBound)) { running ->
                running.engine.check() shouldBe Health.Up
            }
        }

        test("reports a refused bind as an exception with the reason") {
            val failure = shouldThrow<IpcHttpServerBindException> {
                served(endpoint = ScriptedEndpoint(IpcBindResult.AddressInUse)) { }
            }

            failure.result shouldBe IpcBindResult.AddressInUse
            failure.message shouldBe "The engine could not listen on its address: AddressInUse"
        }

        test("happens once") {
            served { running ->
                shouldThrow<IllegalStateException> { running.engine.start(wait = false) }
            }
        }

        test("waits until the engine stops when asked to") {
            served(isStarted = false) { running ->
                coroutineScope {
                    val waiting = async { running.engine.startSuspend(wait = true) }
                    delay(PAUSE.milliseconds)
                    running.engine.stopSuspend(gracePeriodMillis = 0, timeoutMillis = 0)

                    waiting.await() shouldBe running.engine
                }
            }
        }
    }

    context("accepting") {
        test("skips a peer the IPC server refused and serves the next one") {
            served(routes = { get(HELLO) { call.respondText(GREETING) } }) { running ->
                running.endpoint.offer(IpcAcceptResult.PeerRejected(TestIpc.CLIENT))

                running.connect().exchange(get(HELLO)).body shouldBe GREETING
            }
        }

        test("carries on after a failure and gives up after too many in a row") {
            val registry = meterRegistry {}
            val endpoint = ScriptedEndpoint(telemetry = ipcTelemetry { meters(registry) })
            served(endpoint = endpoint) { running ->
                running.endpoint.offer(IpcAcceptResult.Failed(IpcSystemError(code = 1, description = "once")))
                running.endpoint.accepted()
                repeat(FAILURES) {
                    running.endpoint.offer(IpcAcceptResult.Failed(IpcSystemError(code = null, description = "x")))
                }
                while (running.engine.check() == Health.Up) delay(POLL.milliseconds)

                running.engine.check().status shouldBe HealthStatus.Down
                registry.sumOf("konnect.ipc.http_server.failures", address, "reason" to "accept") shouldBe
                    (FAILURES + 1).toDouble()
            }
        }

        test("ends when the endpoint stops listening") {
            served { running ->
                running.endpoint.offer(IpcAcceptResult.NotListening)

                running.engine.check() shouldBe Health.Up
            }
        }
    }

    context("stopping") {
        test("lets a call in flight finish within the grace period and closes its connection") {
            val release = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            served(routes = {
                get(HELLO) {
                    entered.complete(Unit)
                    release.await()
                    call.respondText("late")
                }
            }) { running ->
                val client = running.connect()
                val idle = running.connect()
                idle.exchange(get("/missing")).status shouldBe HttpStatusCode.NotFound.value
                client.send(get(HELLO))
                entered.await()

                coroutineScope {
                    val stopping = launch {
                        running.engine.stopSuspend(
                            gracePeriodMillis = GRACE,
                            timeoutMillis = TIMEOUT,
                        )
                    }
                    idle.rest() shouldBe ""
                    release.complete(Unit)
                    val response = client.response()

                    response.body shouldBe "late"
                    response.headers shouldContain ("connection" to "close")
                    client.rest() shouldBe ""
                    stopping.join()
                }
                running.endpoint.closes shouldBe 1
                running.engine.check().status shouldBe HealthStatus.Down
                running.engine.toString() shouldContain "Stopped"
            }
        }

        test("closes a connection whose answer had started before the engine stopped") {
            val release = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            served(routes = {
                get(HELLO) {
                    call.respondTextWriter {
                        write("head")
                        flush()
                        entered.complete(Unit)
                        release.await()
                        write("tail")
                    }
                }
            }) { running ->
                val client = running.connect()
                client.send(get(HELLO))
                entered.await()

                coroutineScope {
                    val stopping = launch {
                        running.engine.stopSuspend(
                            gracePeriodMillis = GRACE,
                            timeoutMillis = TIMEOUT,
                        )
                    }
                    while (running.endpoint.closes == 0) delay(POLL.milliseconds)
                    delay(PAUSE.milliseconds)
                    release.complete(Unit)

                    client.response().body shouldBe "headtail"
                    client.rest() shouldBe ""
                    stopping.join()
                }
            }
        }

        test("cancels a call that outlives the grace period") {
            val entered = CompletableDeferred<Unit>()
            served(routes = {
                get(HELLO) {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            }) { running ->
                val client = running.connect()
                client.send(get(HELLO))
                entered.await()

                running.engine.stopSuspend(gracePeriodMillis = PAUSE.toLong(), timeoutMillis = 1_000)

                client.rest() shouldBe ""
            }
        }

        test("does nothing for an engine that never started") {
            served(isStarted = false) { running ->
                running.engine.stopSuspend(gracePeriodMillis = 0, timeoutMillis = 0)

                running.engine.check().status shouldBe HealthStatus.Down
                running.engine.toString() shouldContain "Created"
                running.endpoint.closes shouldBe 0
            }
        }

        test("a second time does nothing") {
            served { running ->
                running.engine.stopSuspend(gracePeriodMillis = 0, timeoutMillis = 0)
                running.engine.stopSuspend(gracePeriodMillis = 0, timeoutMillis = 0)

                running.endpoint.closes shouldBe 1
                running.engine.check().status shouldBe HealthStatus.Down
            }
        }
    }

    context("observing") {
        test("counts connections, requests and failures and knows the open connections") {
            val registry = meterRegistry {}
            val endpoint = ScriptedEndpoint(telemetry = ipcTelemetry { meters(registry) })
            served(endpoint = endpoint, routes = { get(HELLO) { call.respondText(GREETING) } }) { running ->
                running.connect().exchange(get(HELLO))
                val open = registry.sumOf("konnect.ipc.http_server.connections.active", address)
                val broken = running.connect()
                broken.exchange("BROKEN\r\n\r\n")
                broken.rest()
                while (running.engine.openConnections > 1) delay(POLL.milliseconds)

                running.engine.openConnections shouldBe 1
                open shouldBe 1.0
                registry.sumOf("konnect.ipc.http_server.connections", address) shouldBe 2.0
                registry.sumOf("konnect.ipc.http_server.requests", address) shouldBe 1.0
                registry.sumOf("konnect.ipc.http_server.failures", address, "reason" to "malformed") shouldBe 1.0
            }
        }
    }
})
