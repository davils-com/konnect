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

import com.davils.konnect.core.Limits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val HTTPS: Int = 443
private const val ABOVE_MAX: Int = 65_536
private const val NOT_DECIMAL: String = "is not a decimal number"
private const val ABOVE_RANGE: String = "is above 65535"

private val categories: Map<Int, PortCategory> = mapOf(
    0 to PortCategory.WellKnown,
    1023 to PortCategory.WellKnown,
    1024 to PortCategory.Registered,
    49_151 to PortCategory.Registered,
    49_152 to PortCategory.Dynamic,
    65_535 to PortCategory.Dynamic,
)

private val refused: Map<String, String> = mapOf(
    "" to "is empty",
    "80a" to NOT_DECIMAL,
    "-1" to NOT_DECIMAL,
    "+1" to NOT_DECIMAL,
    " 80" to NOT_DECIMAL,
    "080" to "has a leading zero",
    "65536" to ABOVE_RANGE,
    "1000000" to ABOVE_RANGE,
    "9".repeat(Limits.MAX_TEXT_LENGTH + 1) to "is longer than 128 characters",
)

class PortTest : FunSpec({
    context("construction") {
        test("keeps its number") {
            Port(HTTPS).value shouldBe HTTPS
            Port(HTTPS).toString() shouldBe "443"
        }

        test("refuses a number outside of the port range") {
            shouldThrow<IllegalArgumentException> { Port(-1) }
            shouldThrow<IllegalArgumentException> { Port(ABOVE_MAX) }
        }

        test("offers the lowest and the highest port") {
            Port.MIN.value shouldBe 0
            Port.MAX.toString() shouldBe "65535"
        }
    }

    context("categories") {
        test("follow the ranges of RFC 6335") {
            categories.forEach { (value, category) ->
                withClue(value) { Port(value).category shouldBe category }
            }
        }
    }

    context("reading") {
        test("reads a decimal number") {
            "443".toPort() shouldBe Port(HTTPS)
            "0".toPortOrNull() shouldBe Port.MIN
            "65535".toPort() shouldBe Port.MAX
            Port.parseOrNull("443") shouldBe Port(HTTPS)
        }

        test("says why a text is no port") {
            refused.forEach { (text, reason) ->
                withClue(text.take(Limits.MAX_TEXT_LENGTH)) {
                    val failure = shouldThrow<IllegalArgumentException> { Port.parse(text) }

                    failure.message shouldBe "Not a port: it $reason"
                    text.toPortOrNull().shouldBeNull()
                }
            }
        }
    }

    context("ordering") {
        test("orders by number") {
            (Port(HTTPS) < Port.MAX) shouldBe true
            listOf(Port.MAX, Port(HTTPS), Port.MIN).sorted() shouldBe listOf(Port.MIN, Port(HTTPS), Port.MAX)
        }
    }
})
