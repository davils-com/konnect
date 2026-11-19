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
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val WEB: String = "8000-8080"
private const val WEB_SIZE: Int = 81
private const val ALL_PORTS: Int = 65_536
private const val HTTP_ALT: Int = 8080
private const val HTTP_DEV: Int = 8000
private const val INSIDE: Int = 8042

private val refused: Map<String, String> = mapOf(
    "8080-8000" to "starts after it ends",
    "1-2-3" to "must be one port or two joined by '-', such as 8000-8080",
    "x-8080" to "has a first port that is not a decimal number",
    "8000-70000" to "has a last port that is above 65535",
    "-8080" to "has a first port that is empty",
    " ".repeat(Limits.MAX_TEXT_LENGTH) + "1" to "is longer than 128 characters",
)

private fun ranges(vararg texts: String): List<PortRange> = texts.map(String::toPortRange)

class PortRangeTest : FunSpec({
    context("reading") {
        test("reads two ports joined by a dash, with or without spaces") {
            val range = "8000 - 8080".toPortRange()

            range.first shouldBe Port(HTTP_DEV)
            range.last shouldBe Port(HTTP_ALT)
            range.toString() shouldBe WEB
        }

        test("reads a single port as a range of one") {
            val range = "443".toPortRange()

            range.size shouldBe 1
            range.toString() shouldBe "443"
        }

        test("says why a text is no port range") {
            refused.forEach { (text, reason) ->
                withClue(text.trim()) {
                    val failure = shouldThrow<IllegalArgumentException> { PortRange.parse(text) }

                    failure.message shouldBe "Not a port range: it $reason"
                    text.toPortRangeOrNull().shouldBeNull()
                }
            }
        }

        test("reads a valid text without failing") {
            PortRange.parseOrNull(WEB) shouldBe WEB.toPortRange()
        }
    }

    context("construction") {
        test("builds a range from its first and last port") {
            PortRange(first = Port(HTTP_DEV), last = Port(HTTP_ALT)) shouldBe WEB.toPortRange()
        }

        test("reports a last port below the first") {
            val failure = shouldThrow<ValidationException> {
                PortRange(first = Port(HTTP_ALT), last = Port(HTTP_DEV))
            }
            val error = failure.report.errors.single()

            error.code shouldBe PortViolationCodes.INVALID_PORT_RANGE
            error.path.toString() shouldBe "last"
            error.message shouldBe "must not be below the first port 8080, but was 8000"
        }

        test("builds a range in the DSL") {
            val range = portRange {
                first = Port(HTTP_DEV)
                last = Port(HTTP_ALT)
            }

            range shouldBe WEB.toPortRange()
        }

        test("defaults to every port in the DSL") {
            portRange {} shouldBe PortRange.ALL
            PortRange.ALL.size shouldBe ALL_PORTS
        }

        test("reports a last port below the first in the DSL") {
            val builder = PortRangeBuilder()
            builder.first = Port.MAX
            builder.last = Port.MIN

            val report = builder.validate()

            report.subject shouldBe "port range"
            report.errors.single().code shouldBe PortViolationCodes.INVALID_PORT_RANGE
        }
    }

    context("calculations") {
        test("counts and lists its ports") {
            val range = WEB.toPortRange()

            range.size shouldBe WEB_SIZE
            range.count() shouldBe WEB_SIZE
            range.first() shouldBe Port(HTTP_DEV)
            range.last() shouldBe Port(HTTP_ALT)
        }

        test("contains the ports and ranges inside of it") {
            val range = WEB.toPortRange()

            (Port(INSIDE) in range) shouldBe true
            (Port.MIN in range) shouldBe false
            (Port.MAX in range) shouldBe false
            ("8010-8020".toPortRange() in range) shouldBe true
            ("7990-8020".toPortRange() in range) shouldBe false
            ("8010-9000".toPortRange() in range) shouldBe false
        }

        test("overlaps ranges that share a port") {
            val range = WEB.toPortRange()

            range.overlaps("8080-9000".toPortRange()) shouldBe true
            range.overlaps("7000-8000".toPortRange()) shouldBe true
            range.overlaps("8081-9000".toPortRange()) shouldBe false
            range.overlaps("7000-7999".toPortRange()) shouldBe false
        }
    }

    context("merging") {
        test("joins overlapping and neighbouring ranges") {
            ranges("9000-9100", "8000-8080", "8081-8090", "8050-8060").merge() shouldContainExactly
                ranges("8000-8090", "9000-9100")
        }

        test("keeps ranges with a gap apart") {
            ranges("22", "80", "443").merge() shouldContainExactly ranges("22", "80", "443")
        }

        test("leaves nothing of nothing") {
            val nothing = emptyList<PortRange>()

            nothing.merge().shouldBeEmpty()
        }
    }

    context("identity") {
        test("equal ranges share a hash code and differ from every other value") {
            WEB.toPortRange().hashCode() shouldBe WEB.toPortRange().hashCode()
            WEB.toPortRange() shouldNotBe "8000-8081".toPortRange()
            WEB.toPortRange() shouldNotBe "7999-8080".toPortRange()
            WEB.toPortRange() shouldNotBe WEB
        }
    }
})
