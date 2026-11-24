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

package com.davils.konnect.ipc.client.builder

import com.davils.konnect.ipc.client.IpcClientViolationCodes
import com.davils.konnect.ipc.client.fixture.HELPER
import com.davils.konnect.ipc.client.fixture.MALLORY
import com.davils.konnect.ipc.core.IpcViolationCodes
import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun clientCodes(block: IpcClientBuilder.() -> Unit): List<ViolationCode> {
    val builder = IpcClientBuilder()
    builder.block()
    return builder.validate().violations.map { violation -> violation.code }
}

class IpcClientBuilderTest : FunSpec({
    context("a client configuration") {
        test("takes its defaults") {
            val builder = IpcClientBuilder()
            builder.address = HELPER
            builder.trust { allow(MALLORY) }
            val configuration = builder.build()

            configuration.trust.users shouldBe listOf(MALLORY)
            configuration.connectTimeout shouldBe IpcClientBuilder.DEFAULT_CONNECT_TIMEOUT
            configuration.connectTimeout shouldBe 5.seconds
            configuration.bufferSize shouldBe Limits.DEFAULT_BUFFER_SIZE
            configuration.telemetry shouldBe IpcTelemetry.None
            configuration.toString() shouldContain "address=user:helper"
        }

        test("keeps what was set") {
            val builder = IpcClientBuilder()
            builder.address = HELPER
            builder.trust = IpcAccess.CURRENT_USER
            builder.connectTimeout = 1.seconds
            builder.bufferSize = Limits.MIN_BUFFER_SIZE
            val configuration = builder.build()

            configuration.address shouldBe HELPER
            configuration.trust shouldBe IpcAccess.CURRENT_USER
            configuration.connectTimeout shouldBe 1.seconds
            configuration.bufferSize shouldBe Limits.MIN_BUFFER_SIZE
        }

        test("reports a timeout that is not positive and finite without quoting it") {
            listOf(Duration.ZERO, -1.seconds, Duration.INFINITE).forEach { timeout ->
                val builder = IpcClientBuilder()
                builder.address = HELPER
                builder.connectTimeout = timeout
                val violation = builder.validate().violations.single()

                violation.code shouldBe IpcClientViolationCodes.INVALID_CONNECT_TIMEOUT
                violation.message shouldBe "must be positive and finite"
            }
        }

        test("reports a buffer outside of its limits") {
            clientCodes {
                address = HELPER
                bufferSize = Limits.MAX_BUFFER_SIZE + 1
            } shouldBe listOf(IpcViolationCodes.INVALID_BUFFER_SIZE)
        }

        test("without an address cannot be used") {
            val configuration = IpcClientConfiguration(IpcClientBuilder())

            configuration.toString() shouldContain "address=none"
            shouldThrow<IllegalStateException> { configuration.address }
            clientCodes { trust { isCurrentUserAllowed = false } } shouldContainExactlyInAnyOrder listOf(
                ViolationCode.REQUIRED,
                IpcViolationCodes.NOBODY_ALLOWED,
            )
        }
    }

    context("a builder that was evaluated") {
        test("refuses every change") {
            val builder = IpcClientBuilder()
            builder.address = HELPER
            builder.build()

            shouldThrow<IllegalStateException> { builder.address = HELPER }
            shouldThrow<IllegalStateException> { builder.trust = IpcAccess.CURRENT_USER }
            shouldThrow<IllegalStateException> { builder.connectTimeout = 1.seconds }
            shouldThrow<IllegalStateException> { builder.bufferSize = Limits.MIN_BUFFER_SIZE }
            shouldThrow<IllegalStateException> { builder.telemetry = IpcTelemetry.None }
            shouldThrow<IllegalStateException> { builder.trust { allow(MALLORY) } }
        }
    }
})
