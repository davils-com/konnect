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

import com.davils.kollect.core.health.HealthStatus
import com.davils.konnect.ipc.server.Limits
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.http.endpoint.ServerEndpoint
import com.davils.konnect.ipc.server.ipcServer
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.validate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

private const val HEADER_SIZE: Int = 128
private const val PORT: Int = 8080
private const val SUBJECT: String = "ipc http server"

private fun unboundServer(dispatcher: CoroutineDispatcher) = ipcServer(dispatcher) { address = SERVICE }

class IpcHttpServerTest : FunSpec({
    context("the configuration") {
        test("is built from its block with the defaults of the limits") {
            val configuration = IpcHttpServer.configuration { maxHeaderSize = HEADER_SIZE }

            configuration.maxHeaderSize shouldBe HEADER_SIZE
            configuration.idleTimeout shouldBe Limits.DEFAULT_IDLE_TIMEOUT_MILLIS.milliseconds
            configuration.requestBodyTimeout shouldBe Limits.DEFAULT_REQUEST_BODY_TIMEOUT_MILLIS.milliseconds
            configuration.maxRequestBodySize shouldBe Limits.DEFAULT_MAX_REQUEST_BODY_SIZE
            configuration.maxRequestsPerConnection shouldBe Limits.DEFAULT_MAX_REQUESTS_PER_CONNECTION
            configuration.toString() shouldContain "maxHeaderSize=128"
            configuration.toString() shouldContain "server=none"
        }

        test("reports every problem at once") {
            val configuration = IpcHttpServer.configuration {
                idleTimeout = Duration.ZERO
                requestBodyTimeout = Duration.INFINITE
                maxHeaderSize = 0
                maxRequestBodySize = -1
                maxRequestsPerConnection = 0
                connector { port = PORT }
            }

            val codes = configuration.validate(SUBJECT).errors.map { violation -> violation.code }

            codes shouldContainExactly listOf(
                IpcHttpServerViolationCodes.MISSING_SERVER,
                IpcHttpServerViolationCodes.INVALID_IDLE_TIMEOUT,
                IpcHttpServerViolationCodes.INVALID_REQUEST_BODY_TIMEOUT,
                IpcHttpServerViolationCodes.INVALID_MAX_HEADER_SIZE,
                IpcHttpServerViolationCodes.INVALID_MAX_REQUEST_BODY_SIZE,
                IpcHttpServerViolationCodes.INVALID_MAX_REQUESTS_PER_CONNECTION,
                IpcHttpServerViolationCodes.UNEXPECTED_CONNECTOR,
            )
        }

        test("refuses every bound beyond its limit with a message that names the limit and not the value") {
            withThreads { threads ->
                val configuration = IpcHttpServer.configuration {
                    server = unboundServer(threads)
                    idleTimeout = 2.hours
                    maxHeaderSize = Limits.MAX_HEADER_SIZE + 1
                    maxRequestBodySize = Limits.MAX_REQUEST_BODY_SIZE + 1
                    maxRequestsPerConnection = Limits.MAX_REQUESTS_PER_CONNECTION + 1
                }

                val violations = configuration.validate(SUBJECT).errors

                violations.map { violation -> violation.code } shouldContainExactly listOf(
                    IpcHttpServerViolationCodes.INVALID_IDLE_TIMEOUT,
                    IpcHttpServerViolationCodes.INVALID_MAX_HEADER_SIZE,
                    IpcHttpServerViolationCodes.INVALID_MAX_REQUEST_BODY_SIZE,
                    IpcHttpServerViolationCodes.INVALID_MAX_REQUESTS_PER_CONNECTION,
                )
                violations.forEach { violation -> violation.message shouldNotContain "${Limits.MAX_HEADER_SIZE + 1}" }
            }
        }

        test("takes every bound at its limit") {
            withThreads { threads ->
                val configuration = IpcHttpServer.configuration {
                    server = unboundServer(threads)
                    idleTimeout = Limits.MAX_TIMEOUT_MILLIS.milliseconds
                    requestBodyTimeout = Limits.MAX_TIMEOUT_MILLIS.milliseconds
                    maxHeaderSize = Limits.MAX_HEADER_SIZE
                    maxRequestBodySize = Limits.MAX_REQUEST_BODY_SIZE
                    maxRequestsPerConnection = Limits.MAX_REQUESTS_PER_CONNECTION
                }

                configuration.validate(SUBJECT).errors shouldBe emptyList()
            }
        }

        test("names the server it serves on") {
            withThreads { threads ->
                val configuration = IpcHttpServer.configuration { server = unboundServer(threads) }

                configuration.toString() shouldContain "server=IpcServer(user:konnect-service, Created)"
            }
        }

        test("hands out no server before it was validated") {
            shouldThrow<IllegalStateException> { IpcHttpServerConfig().boundServer() }
        }
    }

    context("the factory") {
        test("refuses a configuration that names no server") {
            shouldThrow<ValidationException> { embeddedServer(IpcHttpServer, serverConfig { }) }
        }

        test("serves on the IPC server it was given and reports when it cannot listen") {
            withThreads { threads ->
                val ipc = unboundServer(threads)
                ipc.close()
                val server = embeddedServer(IpcHttpServer, serverConfig { parentCoroutineContext = threads }) {
                    this.server = ipc
                }

                val failure = shouldThrow<IpcHttpServerBindException> { server.engine.startSuspend() }

                failure.result shouldBe IpcBindResult.Closed
                server.engine.configuration.server shouldBe ipc
                server.engine.check().status shouldBe HealthStatus.Down
            }
        }
    }

    context("the endpoint of an IPC server") {
        test("hands its calls to the server") {
            withThreads { threads ->
                val ipc = unboundServer(threads)
                val endpoint = ServerEndpoint(ipc)

                endpoint.address shouldBe SERVICE
                endpoint.telemetry shouldBe ipc.configuration.telemetry
                endpoint.accept() shouldBe IpcAcceptResult.NotListening
                endpoint.close()
                endpoint.bind() shouldBe IpcBindResult.Closed
            }
        }
    }

    context("the call attributes") {
        test("are named after Konnect") {
            IpcHttpCallAttributes.peer.name shouldBe "konnect.ipc.peer"
            IpcHttpCallAttributes.address.name shouldBe "konnect.ipc.address"
        }
    }
})
