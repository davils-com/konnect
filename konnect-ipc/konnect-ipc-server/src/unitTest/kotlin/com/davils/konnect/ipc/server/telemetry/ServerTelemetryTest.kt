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

package com.davils.konnect.ipc.server.telemetry

import com.davils.kollect.core.meterRegistry
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.core.telemetry.ipcTelemetry
import com.davils.konnect.ipc.server.IpcServerEvent
import com.davils.konnect.ipc.server.IpcServerTopics
import com.davils.konnect.ipc.server.fixture.EventLog
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.alicePeer
import com.davils.konnect.ipc.server.fixture.sumOf
import com.davils.konnect.ipc.server.fixture.withScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation

private const val EMISSIONS: Int = 8
private const val EVENTS_REJECTED: String = "konnect.ipc.events.rejected"

class ServerTelemetryTest : FunSpec({
    context("events") {
        test("that the bus refuses are counted") {
            withScope { scope ->
                val log = EventLog(scope, capacity = 1)
                log.bus[IpcServerTopics.EVENTS].consume("slow") { handle { awaitCancellation() } }
                val registry = meterRegistry {}
                val telemetry = ServerTelemetry(
                    HELPER,
                    ipcTelemetry {
                        events(log.bus)
                        meters(registry)
                    },
                )

                repeat(EMISSIONS) { telemetry.emit { IpcServerEvent.Closed(HELPER) } }

                registry.sumOf(EVENTS_REJECTED, "address" to HELPER.toString()) shouldBeGreaterThan 0.0
            }
        }

        test("are neither built nor counted without an event bus") {
            val telemetry = ServerTelemetry(HELPER, IpcTelemetry.None)

            telemetry.emit { error("no event is built") }
        }
    }

    context("connections") {
        test("are counted while they are open, without a meter registry too") {
            val telemetry = ServerTelemetry(HELPER, IpcTelemetry.None)

            telemetry.opened(alicePeer)
            telemetry.openConnections shouldBe 1
            telemetry.closed()

            telemetry.openConnections shouldBe 0
        }
    }
})
