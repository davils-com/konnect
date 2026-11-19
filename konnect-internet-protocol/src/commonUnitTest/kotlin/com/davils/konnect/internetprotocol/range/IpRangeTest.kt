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

package com.davils.konnect.internetprotocol.range

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.toIpAddress
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.konnect.internetprotocol.subnet.IpNetwork
import com.davils.konnect.internetprotocol.subnet.toIpv4Network
import com.davils.konnect.internetprotocol.subnet.toIpv6Network
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val SPAN: String = "10.0.0.1-10.0.0.37"
private const val IPV6_SPAN: String = "2001:db8::1-2001:db8::8"
private const val SPAN_SIZE: Long = 37L
private const val FIRST: String = "10.0.0.1"
private const val LAST: String = "10.0.0.37"

private val deaggregated: List<String> = listOf(
    "10.0.0.1/32",
    "10.0.0.2/31",
    "10.0.0.4/30",
    "10.0.0.8/29",
    "10.0.0.16/28",
    "10.0.0.32/30",
    "10.0.0.36/31",
)

private val refused: Map<String, String> = mapOf(
    "10.0.0.9-10.0.0.1" to "starts after it ends",
    "10.0.0.1-10.0.0.2-10.0.0.3" to "must be one address or two joined by '-', such as 192.0.2.1-192.0.2.9",
    "10.0.0-10.0.0.9" to "has a first address that must be four dot-separated numbers, such as 192.0.2.1",
    "10.0.0.1-10.0.0.256" to "has a last address that has a number above 255",
)

class IpRangeTest : FunSpec({
    context("reading") {
        test("reads two addresses joined by a dash, with or without spaces") {
            val range = "$FIRST - $LAST".toIpv4Range()

            range.first shouldBe FIRST.toIpv4Address()
            range.last shouldBe LAST.toIpv4Address()
            range.toString() shouldBe SPAN
            range.version shouldBe IpVersion.V4
        }

        test("reads a single address as a range of one") {
            val range = FIRST.toIpv4Range()

            range.size shouldBe AddressCount.of(1uL)
            range.first shouldBe range.last
        }

        test("says why a text is no IPv4 range") {
            refused.forEach { (text, reason) ->
                withClue(text) {
                    val failure = shouldThrow<IllegalArgumentException> { Ipv4Range.parse(text) }

                    failure.message shouldBe "Not an IPv4 range: it $reason"
                    text.toIpv4RangeOrNull().shouldBeNull()
                }
            }
        }

        test("reads an IPv6 range") {
            val range = IPV6_SPAN.toIpv6Range()

            range.version shouldBe IpVersion.V6
            Ipv6Range.parseOrNull(IPV6_SPAN) shouldBe range
            "2001:db8::9-2001:db8::1".toIpv6RangeOrNull().shouldBeNull()
            shouldThrow<IllegalArgumentException> { Ipv6Range.parse("2001:db8::9-2001:db8::1") }
        }

        test("reads either version") {
            IpRange.parse(SPAN) shouldBe SPAN.toIpv4Range()
            IpRange.parse(IPV6_SPAN) shouldBe IPV6_SPAN.toIpv6Range()
            IpRange.parseOrNull(SPAN) shouldBe SPAN.toIpv4Range()
            IpRange.parseOrNull("::9-::1").shouldBeNull()
            Ipv4Range.parseOrNull(SPAN) shouldBe SPAN.toIpv4Range()
        }
    }

    context("construction") {
        test("builds a range from its first and last address") {
            val ipv4 = Ipv4Range(first = FIRST.toIpv4Address(), last = LAST.toIpv4Address())
            val ipv6 = Ipv6Range(first = "2001:db8::1".toIpv6Address(), last = "2001:db8::8".toIpv6Address())

            ipv4 shouldBe SPAN.toIpv4Range()
            ipv6 shouldBe IPV6_SPAN.toIpv6Range()
        }

        test("reports a last address before the first") {
            val failure = shouldThrow<ValidationException> {
                Ipv4Range(first = LAST.toIpv4Address(), last = FIRST.toIpv4Address())
            }
            val error = failure.report.errors.single()

            error.code shouldBe IpViolationCodes.INVALID_RANGE
            error.path.toString() shouldBe "last"
            error.message shouldBe "must not be before the first address 10.0.0.37, but was 10.0.0.1"
        }
    }

    context("calculations") {
        test("counts its addresses") {
            SPAN.toIpv4Range().size shouldBe AddressCount.of(SPAN_SIZE.toULong())
        }

        test("splits into the fewest networks that cover it exactly") {
            val networks = SPAN.toIpv4Range().toNetworks()

            networks.map(IpNetwork::toString) shouldContainExactly deaggregated
        }

        test("splits an IPv6 range as well") {
            val networks = IPV6_SPAN.toIpv6Range().toNetworks()

            networks.map(IpNetwork::toString) shouldContainExactly
                listOf("2001:db8::1/128", "2001:db8::2/127", "2001:db8::4/126", "2001:db8::8/128")
        }

        test("is a single network when it spans the whole address space") {
            "0.0.0.0-255.255.255.255".toIpv4Range().toNetworks() shouldBe listOf("0.0.0.0/0".toIpv4Network())
        }

        test("is the range of a network") {
            "192.168.1.0/24".toIpv4Network().toRange() shouldBe "192.168.1.0-192.168.1.255".toIpv4Range()
            "2001:db8::/126".toIpv6Network().toRange() shouldBe "2001:db8::-2001:db8::3".toIpv6Range()
        }
    }

    context("containment") {
        test("contains the addresses between its ends") {
            val range = SPAN.toIpv4Range()

            (FIRST.toIpAddress() in range) shouldBe true
            (LAST.toIpAddress() in range) shouldBe true
            ("10.0.0.38".toIpAddress() in range) shouldBe false
            ("10.0.0.0".toIpAddress() in range) shouldBe false
            ("::1".toIpAddress() in range) shouldBe false
        }

        test("contains ranges inside of it") {
            val range = SPAN.toIpv4Range()

            ("10.0.0.5-10.0.0.9".toIpv4Range() in range) shouldBe true
            ("10.0.0.5-10.0.0.99".toIpv4Range() in range) shouldBe false
        }

        test("overlaps ranges that share an address") {
            val range = SPAN.toIpv4Range()

            range.overlaps("10.0.0.37-10.0.0.40".toIpv4Range()) shouldBe true
            range.overlaps("10.0.0.0-10.0.0.1".toIpv4Range()) shouldBe true
            range.overlaps("10.0.0.38-10.0.0.40".toIpv4Range()) shouldBe false
            range.overlaps("10.0.0.0".toIpv4Range()) shouldBe false
            range.overlaps(IPV6_SPAN.toIpv6Range()) shouldBe false
        }
    }

    context("identity") {
        test("equal ranges share a hash code and differ from every other value") {
            SPAN.toIpv4Range().hashCode() shouldBe SPAN.toIpv4Range().hashCode()
            SPAN.toIpv4Range() shouldNotBe "10.0.0.1-10.0.0.36".toIpv4Range()
            SPAN.toIpv4Range() shouldNotBe "10.0.0.2-10.0.0.37".toIpv4Range()
            SPAN.toIpv4Range() shouldNotBe SPAN
        }
    }
})
