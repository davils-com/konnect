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

package com.davils.konnect.ipc.core.telemetry

import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.eventBus
import com.davils.konnect.ipc.core.fixture.RecordingTracer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.coroutineScope

class IpcTelemetryTest : FunSpec({
    context("the telemetry that reports nothing") {
        test("has no meters, no events and the tracer that traces nothing") {
            IpcTelemetry.None.meters.shouldBeNull()
            IpcTelemetry.None.events.shouldBeNull()
            IpcTelemetry.None.tracer shouldBe Tracer.None
            IpcTelemetry.None.toString() shouldBe "IpcTelemetry(meters=false, events=false, traced=false)"
        }
    }

    context("built telemetry") {
        test("holds the meters, the events and the tracer it was given") {
            coroutineScope {
                val registry = meterRegistry {}
                val bus = eventBus(this) {}
                val tracer = RecordingTracer()

                val telemetry = ipcTelemetry {
                    meters(registry)
                    events(bus)
                    this.tracer = tracer
                }

                telemetry.meters shouldBeSameInstanceAs registry
                telemetry.events shouldBeSameInstanceAs bus
                telemetry.tracer shouldBeSameInstanceAs tracer
                telemetry.toString() shouldBe "IpcTelemetry(meters=true, events=true, traced=true)"
                bus.close()
            }
        }

        test("starts from its base and changes only what is set") {
            val tracer = RecordingTracer()
            val base = ipcTelemetry { this.tracer = tracer }
            val registry = meterRegistry {}

            val telemetry = ipcTelemetry(base) { meters(registry) }

            telemetry.tracer shouldBeSameInstanceAs tracer
            telemetry.meters shouldBeSameInstanceAs registry
            telemetry.events.shouldBeNull()
        }

        test("cannot be changed once it was built") {
            val builder = IpcTelemetryBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.tracer = Tracer.None }
            shouldThrow<IllegalStateException> { builder.meters(meterRegistry {}) }
        }
    }
})
