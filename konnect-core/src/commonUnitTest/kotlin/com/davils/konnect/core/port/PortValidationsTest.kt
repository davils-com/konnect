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

package com.davils.konnect.core.port

import com.davils.kore.core.validation.validate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val SUBJECT: String = "listener"

class PortValidationsTest : FunSpec({
    context("port and portRange") {
        test("add nothing to the report for well-formed values and return what they read") {
            val listener = Listener(port = "22", ports = "8000-8080")

            val report = listener.validate(SUBJECT)

            report.violations.shouldBeEmpty()
            listener.readPort shouldBe "22".toPort()
            listener.readPorts shouldBe "8000-8080".toPortRange()
        }

        test("report every malformed value at its path, with its code and a message without the input") {
            val listener = Listener(port = "022", ports = "9-1")

            val report = listener.validate(SUBJECT)

            report.errors.map { it.path.toString() } shouldContainExactly listOf("port", "ports")
            report.errors.map { it.code } shouldContainExactly listOf(
                PortViolationCodes.INVALID_PORT,
                PortViolationCodes.INVALID_PORT_RANGE,
            )
            report.errors.map { it.message } shouldContainExactly listOf(
                "is not a port: it has a leading zero",
                "is not a port range: it starts after it ends",
            )
            listener.readPort shouldBe null
            listener.readPorts shouldBe null
        }
    }

    context("codes") {
        test("keep their stable values") {
            PortViolationCodes.INVALID_PORT.value shouldBe "konnect.port.invalid-port"
            PortViolationCodes.INVALID_PORT_RANGE.value shouldBe "konnect.port.invalid-port-range"
        }
    }
})
