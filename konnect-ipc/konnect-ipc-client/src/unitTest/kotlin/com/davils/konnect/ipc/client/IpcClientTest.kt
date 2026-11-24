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

package com.davils.konnect.ipc.client

import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konfetti.testing.EventRecorder
import com.davils.konfetti.testing.testEventBus
import com.davils.konnect.ipc.client.builder.IpcClientBuilder
import com.davils.konnect.ipc.client.fixture.CONNECTS
import com.davils.konnect.ipc.client.fixture.HELPER
import com.davils.konnect.ipc.client.fixture.RecordingTracer
import com.davils.konnect.ipc.client.fixture.ScriptedTransport
import com.davils.konnect.ipc.client.fixture.alicePeer
import com.davils.konnect.ipc.client.fixture.has
import com.davils.konnect.ipc.client.fixture.malloryPeer
import com.davils.konnect.ipc.client.fixture.sum
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext

private const val RECORDER: String = "recorder"

private fun client(transport: ScriptedTransport, telemetry: IpcTelemetry = IpcTelemetry.None): IpcClient {
    val builder = IpcClientBuilder()
    builder.address = HELPER
    builder.telemetry = telemetry
    return IpcClient(builder.build(), transport)
}

private fun connectedTo(peer: IpcPeer): ScriptedTransport =
    ScriptedTransport(connecting = { ConnectOutcome.Connected(ScriptedChannel(peer = peer)) })

private fun refusing(result: IpcConnectResult): ScriptedTransport =
    ScriptedTransport(connecting = { ConnectOutcome.Refused(result) })

private suspend fun withScope(block: suspend (CoroutineScope) -> Unit) {
    val scope = CoroutineScope(currentCoroutineContext() + Job())
    try {
        block(scope)
    } finally {
        scope.cancel()
    }
}

class IpcClientTest : FunSpec({
    context("connecting") {
        test("hands over a connection to a trusted server") {
            val result = client(connectedTo(alicePeer)).connect()

            val connected = result.shouldBeInstanceOf<IpcConnectResult.Connected>()

            connected.connection.peer shouldBe alicePeer
            connected.connection.isOpen shouldBe true
        }

        test("closes the connection to a server it does not trust") {
            val channel = ScriptedChannel(peer = malloryPeer)
            val client = client(ScriptedTransport(connecting = { ConnectOutcome.Connected(channel) }))

            client.connect() shouldBe IpcConnectResult.ServerRejected(malloryPeer)
            channel.closes shouldBe 1
        }

        test("rejects a server whose user cannot be read") {
            client(connectedTo(IpcPeer.UNKNOWN)).connect() shouldBe IpcConnectResult.ServerRejected(IpcPeer.UNKNOWN)
        }

        test("rejects every server when the current user cannot be read") {
            val unknownSelf = ScriptedTransport(
                connecting = { ConnectOutcome.Connected(ScriptedChannel(peer = alicePeer)) },
                currentUser = null,
            )

            client(unknownSelf).connect() shouldBe IpcConnectResult.ServerRejected(alicePeer)
        }

        test("passes on what the transport refused") {
            client(refusing(IpcConnectResult.NotFound)).connect() shouldBe IpcConnectResult.NotFound
            client(refusing(IpcConnectResult.NotFound)).toString() shouldBe "IpcClient(user:helper)"
        }

        test("serves as a connector") {
            val connector: IpcConnector = client(refusing(IpcConnectResult.TimedOut))

            connector.connect() shouldBe IpcConnectResult.TimedOut
        }
    }

    context("the span of an attempt") {
        test("is a client span with the address and the outcome that ends as a success") {
            val tracer = RecordingTracer()
            val telemetry = ipcTelemetry { this.tracer = tracer }

            client(connectedTo(alicePeer), telemetry).connect()

            tracer.operations shouldBe listOf("ipc.connect" to SpanKind.Client)
            tracer.attributes shouldBe mapOf("ipc.address" to "user:helper", "ipc.outcome" to "connected")
            tracer.outcomes shouldBe listOf(SpanOutcome.Ok)
        }

        test("names a refusal as its outcome") {
            val tracer = RecordingTracer()

            client(refusing(IpcConnectResult.AccessDenied), ipcTelemetry { this.tracer = tracer }).connect()

            tracer.attributes["ipc.outcome"] shouldBe "access_denied"
        }
    }

    context("the counter of attempts") {
        test("counts every attempt by its outcome") {
            val registry = meterRegistry()
            val telemetry = ipcTelemetry { meters(registry) }

            client(connectedTo(alicePeer), telemetry).connect()
            client(connectedTo(malloryPeer), telemetry).connect()
            client(refusing(IpcConnectResult.NotFound), telemetry).connect()
            client(refusing(IpcConnectResult.NotFound), telemetry).connect()

            registry.sum(CONNECTS, "connected") shouldBe 1.0
            registry.sum(CONNECTS, "server_rejected") shouldBe 1.0
            registry.sum(CONNECTS, "not_found") shouldBe 2.0
        }

        test("is not registered without a meter registry") {
            val registry = meterRegistry()

            client(connectedTo(alicePeer)).connect()

            registry.has(CONNECTS) shouldBe false
        }
    }

    context("the event of a rejected server") {
        test("reaches the topic of the event bus") {
            withScope { scope ->
                val bus = testEventBus(scope)
                bus.declare(IpcClientTopics.EVENTS)
                val recorder = EventRecorder<IpcClientEvent>()
                bus[IpcClientTopics.EVENTS].consume(RECORDER) { handle(recorder::record) }

                client(connectedTo(malloryPeer), ipcTelemetry { events(bus) }).connect()

                recorder.awaitCount(1)
                recorder.payloads shouldBe listOf(IpcClientEvent.ServerRejected(HELPER, malloryPeer))
            }
        }

        test("declares its topic when the bus does not") {
            withScope { scope ->
                val bus = testEventBus(scope)

                client(connectedTo(malloryPeer), ipcTelemetry { events(bus) }).connect()

                (IpcClientTopics.EVENTS in bus) shouldBe true
            }
        }

        test("is not published for a trusted server") {
            withScope { scope ->
                val bus = testEventBus(scope)

                client(connectedTo(alicePeer), ipcTelemetry { events(bus) }).connect()

                (IpcClientTopics.EVENTS in bus) shouldBe false
            }
        }

        test("never fails an attempt when the bus is closed") {
            withScope { scope ->
                val bus = testEventBus(scope)
                bus.abort()

                val result = client(connectedTo(malloryPeer), ipcTelemetry { events(bus) }).connect()

                result shouldBe IpcConnectResult.ServerRejected(malloryPeer)
            }
        }
    }

    context("an event") {
        test("is equal to an event of the same address and peer and shows both") {
            val event = IpcClientEvent.ServerRejected(HELPER, malloryPeer)

            event shouldBe IpcClientEvent.ServerRejected(HELPER, malloryPeer)
            (event == IpcClientEvent.ServerRejected(HELPER, alicePeer)) shouldBe false
            event.equals(HELPER) shouldBe false
            (event == IpcClientEvent.ServerRejected(IpcAddress.WindowsPipe("other"), malloryPeer)) shouldBe false
            event.hashCode() shouldBe IpcClientEvent.ServerRejected(HELPER, malloryPeer).hashCode()
            event.toString() shouldBe "ServerRejected(address=user:helper, peer=$malloryPeer)"
            IpcClientTopics.EVENTS.name.value shouldBe "konnect.ipc.client"
        }
    }
})
