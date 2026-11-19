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

package com.davils.konnect.internetprotocol.address

import com.davils.konnect.internetprotocol.IpVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val DOCUMENTATION: String = "2001:db8::1"
private const val STEP: Long = 0x10000L
private const val GROUP_LIMIT: Int = 0x10000
private const val MAX: String = "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"

private val documentationGroups: List<Int> = listOf(0x2001, 0xdb8, 0, 0, 0, 0, 0, 1)

private val canonical: Map<String, String> = mapOf(
    "2001:DB8:0:0:8:800:200C:417A" to "2001:db8::8:800:200c:417a",
    "FF01:0:0:0:0:0:0:101" to "ff01::101",
    "0:0:0:0:0:0:0:1" to "::1",
    "0:0:0:0:0:0:0:0" to "::",
    "::13.1.68.3" to "::d01:4403",
    "::FFFF:129.144.52.38" to "::ffff:129.144.52.38",
    "2001:0db8:0:0:0:0:2:1" to "2001:db8::2:1",
    "2001:db8:0:0:1:0:0:1" to "2001:db8::1:0:0:1",
    "2001:db8:0:0:1:0:0:0" to "2001:db8:0:0:1::",
    "2001:db8:0:1:1:1:1:1" to "2001:db8:0:1:1:1:1:1",
    "1:2:3:4:5:6:7::" to "1:2:3:4:5:6:7:0",
    "::1:2:3:4:5:6:7" to "0:1:2:3:4:5:6:7",
    "1:2:3:4:5:6:1.2.3.4" to "1:2:3:4:5:6:102:304",
    MAX.uppercase() to MAX,
)

private const val COUNT: String = "must have eight colon-separated groups, or '::' for a run of zeros"
private const val GROUP: String = "has a group that is not one to four hexadecimal digits"

private val refused: Map<String, String> = mapOf(
    "" to "is empty",
    "0000:1111:2222:3333:4444:5555:6666:7777:8888:9" to "is longer than 45 characters",
    "fe80::1%eth0" to "has a zone index, which is not supported",
    "1::2::3" to "uses '::' more than once",
    "1:2:3:4:5:6:7" to COUNT,
    "1:2:3:4:5:6:7:8:9" to COUNT,
    "1:2:3:4:5:6:7:1.2.3.4" to COUNT,
    "1:2:3:4::5:6:7:8" to "has more than seven groups beside '::'",
    "12345::" to GROUP,
    "g::" to GROUP,
    ":1:2:3:4:5:6:7" to GROUP,
    "1.2.3.4::" to GROUP,
    "1.2.3.4:1::" to GROUP,
    ":::" to GROUP,
    "::1.2.3" to "has an embedded IPv4 address that must be four dot-separated numbers, such as 192.0.2.1",
    "::256.1.1.1" to "has an embedded IPv4 address that has a number above 255",
)

class Ipv6AddressTest : FunSpec({
    context("reading") {
        test("reads the forms of RFC 4291 and writes them as RFC 5952 recommends") {
            canonical.forEach { (text, expected) ->
                withClue(text) { Ipv6Address.parse(text).toString() shouldBe expected }
            }
        }

        test("says why a text is no IPv6 address") {
            refused.forEach { (text, reason) ->
                withClue(text) {
                    val failure = shouldThrow<IllegalArgumentException> { Ipv6Address.parse(text) }

                    failure.message shouldBe "Not an IPv6 address: it $reason"
                    Ipv6Address.parseOrNull(text).shouldBeNull()
                }
            }
        }

        test("reads a valid text without failing") {
            Ipv6Address.parseOrNull(DOCUMENTATION) shouldBe Ipv6Address.parse(DOCUMENTATION)
        }
    }

    context("construction") {
        test("builds an address from its groups") {
            Ipv6Address(documentationGroups).toString() shouldBe DOCUMENTATION
        }

        test("refuses any other number of groups") {
            shouldThrow<IllegalArgumentException> { Ipv6Address(listOf(1, 2)) }
        }

        test("refuses a group outside of sixteen bits") {
            val negative = listOf(-1) + documentationGroups.drop(1)
            val tooLarge = listOf(GROUP_LIMIT) + documentationGroups.drop(1)

            shouldThrow<IllegalArgumentException> { Ipv6Address(negative) }
            shouldThrow<IllegalArgumentException> { Ipv6Address(tooLarge) }
        }

        test("reads back from its bytes") {
            val address = DOCUMENTATION.toIpv6Address()

            Ipv6Address.fromByteArray(address.toByteArray()) shouldBe address
        }

        test("refuses bytes of any other length") {
            shouldThrow<IllegalArgumentException> { Ipv6Address.fromByteArray(ByteArray(size = 4)) }
        }

        test("offers the well-known addresses") {
            Ipv6Address.UNSPECIFIED.toString() shouldBe "::"
            Ipv6Address.LOOPBACK.toString() shouldBe "::1"
        }
    }

    context("parts") {
        test("is version six") {
            DOCUMENTATION.toIpv6Address().version shouldBe IpVersion.V6
        }

        test("lists its groups from the most significant") {
            DOCUMENTATION.toIpv6Address().groups shouldContainExactly documentationGroups
        }

        test("writes every group in full") {
            DOCUMENTATION.toIpv6Address().toFullString() shouldBe "2001:0db8:0000:0000:0000:0000:0000:0001"
        }
    }

    context("IPv4 inside IPv6") {
        test("finds the IPv4 address of a mapped address") {
            "::ffff:192.0.2.1".toIpv6Address().toIpv4OrNull() shouldBe "192.0.2.1".toIpv4Address()
        }

        test("finds none in any other address") {
            val unmapped = listOf(DOCUMENTATION, "1::ffff:192.0.2.1").map(String::toIpv6Address)

            unmapped.map(Ipv6Address::toIpv4OrNull) shouldBe listOf(null, null)
        }
    }

    context("arithmetic") {
        test("carries from the low into the high half") {
            "::ffff:ffff:ffff:ffff".toIpv6Address().next() shouldBe "0:0:0:1::".toIpv6Address()
        }

        test("borrows from the high into the low half") {
            "0:0:0:1::".toIpv6Address().previous() shouldBe "::ffff:ffff:ffff:ffff".toIpv6Address()
        }

        test("shifts by any offset in both directions") {
            "2001:db8::".toIpv6Address().shiftedBy(STEP) shouldBe "2001:db8::1:0".toIpv6Address()
            "2001:db8::1:0".toIpv6Address().shiftedBy(-STEP) shouldBe "2001:db8::".toIpv6Address()
        }

        test("has no address beyond the ends of the address space") {
            MAX.toIpv6Address().next() shouldBe null
            Ipv6Address.UNSPECIFIED.previous().shouldBeNull()
        }
    }

    context("representations") {
        test("writes its bits group by group") {
            DOCUMENTATION.toIpv6Address().toBinaryString() shouldBe
                "0010000000000001:0000110110111000:0000000000000000:0000000000000000:" +
                "0000000000000000:0000000000000000:0000000000000000:0000000000000001"
        }

        test("writes its bits in hexadecimal") {
            DOCUMENTATION.toIpv6Address().toHexString() shouldBe "20010db8000000000000000000000001"
        }

        test("names its reverse DNS entry nibble by nibble") {
            DOCUMENTATION.toIpv6Address().reverseDnsName shouldBe
                "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa"
        }
    }
})
