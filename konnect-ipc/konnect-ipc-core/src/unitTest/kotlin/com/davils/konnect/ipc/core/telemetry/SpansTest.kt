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

package com.davils.konnect.ipc.core.telemetry

import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konnect.ipc.core.fixture.RecordingTracer
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException

private const val OPERATION: String = "work"
private const val ERROR_TYPE: String = "error.type"

class SpansTest : FunSpec({
    context("a traced block") {
        test("ends its span as a success when it returns") {
            val tracer = RecordingTracer()

            tracer.start(OPERATION).traced { OPERATION } shouldBe OPERATION

            tracer.outcomes shouldBe listOf(SpanOutcome.Ok)
            tracer.attributes.shouldBeEmpty()
        }

        test("ends its span as a failure when it throws") {
            val tracer = RecordingTracer()

            shouldThrow<IllegalArgumentException> { tracer.start(OPERATION).traced { require(false) } }

            tracer.outcomes shouldBe listOf(SpanOutcome.Error)
            tracer.attributes shouldBe mapOf(ERROR_TYPE to "failed")
        }

        test("ends its span as cancelled only when it is cancelled") {
            val tracer = RecordingTracer()

            shouldThrow<CancellationException> {
                tracer.start(OPERATION).traced { throw CancellationException(OPERATION) }
            }

            tracer.outcomes shouldBe listOf(SpanOutcome.Error)
            tracer.attributes shouldBe mapOf(ERROR_TYPE to "cancelled")
        }
    }
})
