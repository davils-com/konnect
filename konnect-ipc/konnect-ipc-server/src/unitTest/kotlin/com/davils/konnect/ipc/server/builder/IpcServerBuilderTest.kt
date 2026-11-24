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

package com.davils.konnect.ipc.server.builder

import com.davils.konnect.ipc.core.IpcViolationCodes
import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.IpcServerViolationCodes
import com.davils.konnect.ipc.server.Limits
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.MALLORY
import com.davils.konnect.ipc.server.fixture.shouldNotEcho
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import com.davils.konnect.ipc.core.Limits as CoreLimits

private const val CASES: Int = 1_000
private const val TOO_SMALL: Int = CoreLimits.MIN_BUFFER_SIZE - 1

private fun serverCodes(block: IpcServerBuilder.() -> Unit): List<ViolationCode> {
    val builder = IpcServerBuilder()
    builder.block()
    return builder.validate().violations.map { violation -> violation.code }
}

class IpcServerBuilderTest : FunSpec({
    context("a server configuration") {
        test("takes its defaults") {
            val builder = IpcServerBuilder()
            builder.address = HELPER
            val configuration = builder.build()

            configuration.address shouldBe HELPER
            configuration.access shouldBe IpcAccess.CURRENT_USER
            configuration.maxConnections shouldBe Limits.DEFAULT_MAX_CONNECTIONS
            configuration.bufferSize shouldBe CoreLimits.DEFAULT_BUFFER_SIZE
            configuration.telemetry shouldBe IpcTelemetry.None
            configuration.toString() shouldContain "address=user:helper"
        }

        test("reports every problem at once") {
            val codes = serverCodes {
                maxConnections = 0
                bufferSize = TOO_SMALL
                access { isCurrentUserAllowed = false }
            }

            codes shouldContainExactlyInAnyOrder listOf(
                ViolationCode.REQUIRED,
                IpcServerViolationCodes.INVALID_MAX_CONNECTIONS,
                IpcViolationCodes.INVALID_BUFFER_SIZE,
                IpcViolationCodes.NOBODY_ALLOWED,
            )
        }

        test("refuses more connections than the hard ceiling") {
            val codes = serverCodes {
                address = HELPER
                maxConnections = Limits.MAX_CONNECTIONS + 1
            }

            codes shouldBe listOf(IpcServerViolationCodes.INVALID_MAX_CONNECTIONS)
        }

        test("takes the hard ceiling itself") {
            serverCodes {
                address = HELPER
                maxConnections = Limits.MAX_CONNECTIONS
            } shouldBe emptyList()
        }

        test("keeps the access it was given") {
            val builder = IpcServerBuilder()
            builder.address = HELPER
            builder.access { allow(MALLORY) }

            builder.build().access.users shouldBe listOf(MALLORY)
        }

        test("without an address cannot be used") {
            val configuration = IpcServerConfiguration(IpcServerBuilder())

            configuration.toString() shouldContain "address=none"
            shouldThrow<IllegalStateException> { configuration.address }
            shouldThrow<ValidationException> { IpcServerBuilder().build() }
        }

        test("refuses a number of connections with a message that never quotes it") {
            checkAll(CASES, Arb.int()) { connections ->
                val builder = IpcServerBuilder()
                builder.address = HELPER
                builder.maxConnections = connections

                builder.validate().violations.forEach { violation ->
                    violation.message.shouldNotEcho(connections.toString())
                    violation.message shouldContain Limits.MAX_CONNECTIONS.toString()
                }
            }
        }
    }
})
