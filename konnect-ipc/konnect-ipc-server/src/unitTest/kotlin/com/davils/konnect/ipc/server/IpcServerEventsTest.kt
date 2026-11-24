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

import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.core.TopicKey
import com.davils.konfetti.testing.testEventBus
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.ipc.server.builder.IpcServerBuilder
import com.davils.konnect.ipc.server.fixture.EventLog
import com.davils.konnect.ipc.server.fixture.ForeignEvent
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.ScriptedListener
import com.davils.konnect.ipc.server.fixture.ScriptedTransport
import com.davils.konnect.ipc.server.fixture.alicePeer
import com.davils.konnect.ipc.server.fixture.malloryPeer
import com.davils.konnect.ipc.server.fixture.sumOf
import com.davils.konnect.ipc.server.fixture.withScope
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val EVENTS_REJECTED: String = "konnect.ipc.events.rejected"
private val address: Pair<String, String> = "address" to "user:helper"

private fun server(transport: ScriptedTransport, block: IpcServerBuilder.() -> Unit): IpcServer {
    val builder = IpcServerBuilder()
    builder.address = HELPER
    builder.block()
    return IpcServer(builder.build(), transport)
}

private fun accepting(vararg peers: IpcPeer): ScriptedTransport {
    val outcomes = peers.map { peer -> AcceptOutcome.Accepted(ScriptedChannel(peer)) }
    return ScriptedTransport(listening = { ListenOutcome.Listening(ScriptedListener(outcomes)) })
}

class IpcServerEventsTest : FunSpec({
    context("events") {
        test("tell consumers what the server did, a refused peer included") {
            withScope { scope ->
                val log = EventLog(scope)
                log.record()
                val server = server(accepting(alicePeer, malloryPeer)) { telemetry = ipcTelemetry { events(log.bus) } }

                server.bind()
                server.accept()
                server.accept()
                server.close()

                log.events(count = 4) shouldContainExactly listOf(
                    IpcServerEvent.Bound(HELPER),
                    IpcServerEvent.Accepted(HELPER, alicePeer),
                    IpcServerEvent.PeerRejected(HELPER, malloryPeer),
                    IpcServerEvent.Closed(HELPER),
                )
            }
        }

        test("are not built while the topic has no consumer, and the topic is declared on first use") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val server = server(accepting()) { telemetry = ipcTelemetry { events(bus) } }

                server.bind()

                (IpcServerTopics.EVENTS in bus) shouldBe true
            }
        }

        test("on a topic of another type are counted as refused") {
            withScope { scope ->
                val foreign = TopicKey<ForeignEvent>(IpcServerTopics.EVENTS.name.value)
                val bus = testEventBus(scope) { topic(foreign) }
                val registry = meterRegistry {}
                val server = server(accepting()) {
                    telemetry = ipcTelemetry {
                        events(bus)
                        meters(registry)
                    }
                }

                server.bind()

                registry.sumOf(EVENTS_REJECTED, address) shouldBe 1.0
            }
        }
    }

    context("the topic of events") {
        test("carries the events of every server") {
            IpcServerTopics.EVENTS.eventType shouldBe IpcServerEvent::class
        }
    }
})
