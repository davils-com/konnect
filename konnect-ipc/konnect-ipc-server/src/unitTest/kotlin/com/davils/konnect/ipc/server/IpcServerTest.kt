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

package com.davils.konnect.ipc.server

import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.ipc.server.builder.IpcServerBuilder
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.RecordingTracer
import com.davils.konnect.ipc.server.fixture.ScriptedListener
import com.davils.konnect.ipc.server.fixture.ScriptedTransport
import com.davils.konnect.ipc.server.fixture.alicePeer
import com.davils.konnect.ipc.server.fixture.malloryPeer
import com.davils.konnect.ipc.server.fixture.sumOf
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val ADDRESS: String = "user:helper"
private const val ACTIVE: String = "konnect.ipc.connections.active"
private const val CONNECTIONS: String = "konnect.ipc.connections"
private val LABEL: Pair<String, String> = "address" to ADDRESS

private fun server(transport: ScriptedTransport, block: IpcServerBuilder.() -> Unit = {}): IpcServer {
    val builder = IpcServerBuilder()
    builder.address = HELPER
    builder.block()
    return IpcServer(builder.build(), transport)
}

private fun listeningOn(listener: ScriptedListener): ScriptedTransport =
    ScriptedTransport(listening = { ListenOutcome.Listening(listener) })

private fun accepting(vararg outcomes: AcceptOutcome): ScriptedTransport =
    listeningOn(ScriptedListener(outcomes.toList()))

private fun acceptedFrom(channel: ScriptedChannel): AcceptOutcome = AcceptOutcome.Accepted(channel)

private fun listenerOf(peer: IpcPeer): ListenOutcome {
    val listener = ScriptedListener(listOf(acceptedFrom(ScriptedChannel(peer))))
    return ListenOutcome.Listening(listener)
}

class IpcServerTest : FunSpec({
    context("binding") {
        test("starts listening once") {
            val transport = accepting()
            val server = server(transport)

            server.bind() shouldBe IpcBindResult.Bound
            server.bind() shouldBe IpcBindResult.AlreadyBound
            server.state.value shouldBe IpcServerState.Listening
            transport.listens shouldBe 1
        }

        test("passes on what the transport refused") {
            val server = server(ScriptedTransport(listening = { ListenOutcome.Refused(IpcBindResult.AddressInUse) }))

            server.bind() shouldBe IpcBindResult.AddressInUse
            server.state.value shouldBe IpcServerState.Created
        }

        test("is refused once the server was closed") {
            val server = server(accepting())
            server.close()

            server.bind() shouldBe IpcBindResult.Closed
        }

        test("gives up a listener that arrives after the server was closed") {
            val listener = ScriptedListener()
            var closing: IpcServer? = null
            val server = server(
                ScriptedTransport(
                    listening = {
                        closing?.close()
                        ListenOutcome.Listening(listener)
                    },
                ),
            )
            closing = server

            server.bind() shouldBe IpcBindResult.Closed
            listener.closes shouldBe 1
        }
    }

    context("accepting") {
        test("needs a bound server") {
            server(accepting()).accept() shouldBe IpcAcceptResult.NotListening
        }

        test("hands over a connection of a permitted peer") {
            val server = server(accepting(acceptedFrom(ScriptedChannel(alicePeer))))
            server.bind()

            val accepted = server.accept().shouldBeInstanceOf<IpcAcceptResult.Accepted>()

            accepted.connection.peer shouldBe alicePeer
            server.openConnections shouldBe 1
            accepted.connection.close()
            server.openConnections shouldBe 0
        }

        test("closes the connection of a peer that is not permitted and names it on the span") {
            val channel = ScriptedChannel(malloryPeer)
            val tracer = RecordingTracer()
            val server = server(accepting(acceptedFrom(channel))) { telemetry = ipcTelemetry { this.tracer = tracer } }
            server.bind()

            server.accept() shouldBe IpcAcceptResult.PeerRejected(malloryPeer)

            channel.closes shouldBe 1
            tracer.operations shouldBe listOf("ipc.accept")
            tracer.attributes shouldBe mapOf("ipc.address" to ADDRESS, "ipc.peer" to malloryPeer.toString())
            tracer.outcomes shouldBe listOf(SpanOutcome.Ok)
        }

        test("refuses a peer whose user cannot be read and a server whose own user is unknown") {
            val unknown = ScriptedChannel(IpcPeer.UNKNOWN)
            val anonymous = server(ScriptedTransport(listening = { listenerOf(alicePeer) }, currentUser = null))
            val server = server(accepting(acceptedFrom(unknown)))
            server.bind()
            anonymous.bind()

            server.accept().shouldBeInstanceOf<IpcAcceptResult.PeerRejected>()
            anonymous.accept().shouldBeInstanceOf<IpcAcceptResult.PeerRejected>()
        }

        test("passes on what the transport refused") {
            val server = server(accepting(AcceptOutcome.Refused(IpcAcceptResult.NotListening)))
            server.bind()

            server.accept() shouldBe IpcAcceptResult.NotListening
        }

        test("waits for a free slot when every connection is taken") {
            val listener = ScriptedListener(
                listOf(acceptedFrom(ScriptedChannel(alicePeer)), acceptedFrom(ScriptedChannel(alicePeer))),
            )
            val server = server(listeningOn(listener)) { maxConnections = 1 }
            server.bind()
            val first = server.accept().shouldBeInstanceOf<IpcAcceptResult.Accepted>()

            coroutineScope {
                val second = async { server.accept() }
                yield()
                second.isCompleted shouldBe false
                first.connection.close()
                second.await().shouldBeInstanceOf<IpcAcceptResult.Accepted>()
            }
        }

        test("frees the slot of a cancelled accept") {
            val listener = ScriptedListener()
            val tracer = RecordingTracer()
            val server = server(listeningOn(listener)) {
                maxConnections = 1
                telemetry = ipcTelemetry { this.tracer = tracer }
            }
            server.bind()

            coroutineScope {
                val waiting = launch { server.accept() }
                yield()
                waiting.cancelAndJoin()
            }
            listener.add(acceptedFrom(ScriptedChannel(alicePeer)))

            server.accept().shouldBeInstanceOf<IpcAcceptResult.Accepted>()
            tracer.outcomes shouldBe listOf(SpanOutcome.Error, SpanOutcome.Ok)
        }
    }

    context("closing") {
        test("an unbound server only marks it as closed") {
            val server = server(accepting())

            server.close()

            server.state.value shouldBe IpcServerState.Closed
        }

        test("closes the listener once and marks the server as down") {
            val listener = ScriptedListener()
            val server = server(listeningOn(listener))
            server.check() shouldBe Health.Up
            server.bind()

            server.close()
            server.close()

            listener.closes shouldBe 1
            server.state.value shouldBe IpcServerState.Closed
            server.check().status shouldBe HealthStatus.Down
            server.accept() shouldBe IpcAcceptResult.NotListening
            server.toString() shouldBe "IpcServer(user:helper, Closed)"
        }
    }

    context("meters") {
        test("count the connections of the server by outcome and the open ones") {
            val registry = meterRegistry {}
            val server = server(
                accepting(acceptedFrom(ScriptedChannel(alicePeer)), acceptedFrom(ScriptedChannel(malloryPeer))),
            ) { telemetry = ipcTelemetry { meters(registry) } }
            server.bind()

            val accepted = server.accept().shouldBeInstanceOf<IpcAcceptResult.Accepted>()
            server.accept()

            registry.sumOf(ACTIVE, LABEL) shouldBe 1.0
            registry.sumOf(CONNECTIONS, LABEL, "outcome" to "accepted") shouldBe 1.0
            registry.sumOf(CONNECTIONS, LABEL, "outcome" to "rejected") shouldBe 1.0
            accepted.connection.close()
            registry.sumOf(ACTIVE, LABEL) shouldBe 0.0
        }
    }
})
