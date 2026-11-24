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

package com.davils.konnect.ipc.client.http

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.Limits
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.validate
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val SUBJECT: String = "engine"
private const val HOSTILE_COUNT: Int = 987_654

private fun violationsOf(block: IpcHttpConfig.() -> Unit): List<String> {
    val config = IpcHttpConfig()
    config.block()
    return config.validate(SUBJECT).violations.map { violation -> violation.path.toString() }
}

class IpcHttpConfigTest : FunSpec({
    context("a configuration") {
        test("takes its defaults from the limits") {
            val config = IpcHttpConfig()

            config.maxConnections shouldBe Limits.DEFAULT_MAX_CONNECTIONS
            config.maxIdleConnections shouldBe Limits.DEFAULT_MAX_IDLE_CONNECTIONS
            config.idleTimeout shouldBe 30.seconds
            config.maxResponseHeadSize shouldBe Limits.DEFAULT_MAX_RESPONSE_HEAD_SIZE
            config.connector.connect().shouldBeInstanceOf<IpcConnectResult.Unavailable>()
            violationsOf {} shouldBe emptyList()
        }

        test("accepts the bounds of every limit") {
            violationsOf {
                maxConnections = Limits.MAX_CONNECTIONS
                maxIdleConnections = 0
                idleTimeout = 1.seconds
                maxResponseHeadSize = Limits.MIN_RESPONSE_HEAD_SIZE
            } shouldBe emptyList()
            violationsOf {
                maxConnections = 1
                maxIdleConnections = Limits.MAX_IDLE_CONNECTIONS
                maxResponseHeadSize = Limits.MAX_RESPONSE_HEAD_SIZE
            } shouldBe emptyList()
        }

        test("reports every value outside of its limits") {
            violationsOf {
                maxConnections = 0
                maxIdleConnections = -1
                idleTimeout = Duration.INFINITE
                maxResponseHeadSize = Limits.MAX_RESPONSE_HEAD_SIZE + 1
            } shouldBe listOf("maxConnections", "maxIdleConnections", "idleTimeout", "maxResponseHeadSize")
        }

        test("is refused when the engine is created, without quoting the value") {
            val failure = shouldThrow<ValidationException> {
                IpcHttp.create { maxConnections = HOSTILE_COUNT }.use { engine -> engine.config }
            }

            failure.report.violations
                .single()
                .code shouldBe ViolationCode.OUT_OF_RANGE
            failure.message shouldNotContain HOSTILE_COUNT.toString()
        }
    }
})
