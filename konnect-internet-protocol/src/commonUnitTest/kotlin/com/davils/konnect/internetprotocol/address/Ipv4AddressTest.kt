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

private const val STEP: Long = 256L
private const val HOST: String = "192.168.1.10"

private val hostOctets: List<Int> = listOf(192, 168, 1, 10)

private const val BROADCAST: String = "255.255.255.255"
private const val DOCUMENTATION: String = "192.0.2.1"

private val valid: List<String> = listOf("0.0.0.0", DOCUMENTATION, "10.0.0.255", BROADCAST, HOST)

private const val SHAPE: String = "must be four dot-separated numbers, such as 192.0.2.1"
private const val NOT_DECIMAL: String = "has a part that is not a decimal number"
private const val ABOVE_BYTE: String = "has a number above 255"

private val refused: Map<String, String> = mapOf(
    "" to "is empty",
    "1.2.3" to SHAPE,
    "1.2.3.4.5" to SHAPE,
    "1.2.3.a" to NOT_DECIMAL,
    "1..3.4" to NOT_DECIMAL,
    "1.2.3.-4" to NOT_DECIMAL,
    "01.2.3.4" to "has a number with a leading zero",
    "256.1.1.1" to ABOVE_BYTE,
    "1000.1.1.1" to ABOVE_BYTE,
)

private val classes: Map<String, Ipv4Class> = mapOf(
    "10.0.0.1" to Ipv4Class.A,
    "127.255.255.255" to Ipv4Class.A,
    "128.0.0.0" to Ipv4Class.B,
    "191.255.0.0" to Ipv4Class.B,
    "192.0.0.0" to Ipv4Class.C,
    "223.255.255.0" to Ipv4Class.C,
    "224.0.0.1" to Ipv4Class.D,
    "239.255.255.255" to Ipv4Class.D,
    "240.0.0.0" to Ipv4Class.E,
    BROADCAST to Ipv4Class.E,
)

class Ipv4AddressTest : FunSpec({
    context("reading") {
        test("reads every dotted quad back to the same text") {
            valid.forEach { text -> Ipv4Address.parse(text).toString() shouldBe text }
        }

        test("says why a text is no IPv4 address") {
            refused.forEach { (text, reason) ->
                withClue(text) {
                    val failure = shouldThrow<IllegalArgumentException> { Ipv4Address.parse(text) }

                    failure.message shouldBe "Not an IPv4 address: it $reason"
                    Ipv4Address.parseOrNull(text).shouldBeNull()
                }
            }
        }

        test("reads a valid text without failing") {
            Ipv4Address.parseOrNull(HOST) shouldBe Ipv4Address.parse(HOST)
        }
    }

    context("construction") {
        test("builds an address from its octets") {
            Ipv4Address(first = 192, second = 168, third = 1, fourth = 10).toString() shouldBe HOST
        }

        test("refuses an octet outside of a byte") {
            shouldThrow<IllegalArgumentException> { Ipv4Address(first = 256, second = 0, third = 0, fourth = 0) }
            shouldThrow<IllegalArgumentException> { Ipv4Address(first = 0, second = 0, third = 0, fourth = -1) }
        }

        test("reads back from its bytes") {
            val address = HOST.toIpv4Address()

            Ipv4Address.fromByteArray(address.toByteArray()) shouldBe address
        }

        test("refuses bytes of any other length") {
            shouldThrow<IllegalArgumentException> { Ipv4Address.fromByteArray(ByteArray(size = 16)) }
        }

        test("offers the well-known addresses") {
            Ipv4Address.UNSPECIFIED.toString() shouldBe "0.0.0.0"
            Ipv4Address.LOOPBACK.toString() shouldBe "127.0.0.1"
            Ipv4Address.BROADCAST.toString() shouldBe BROADCAST
        }
    }

    context("parts") {
        test("is version four") {
            HOST.toIpv4Address().version shouldBe IpVersion.V4
        }

        test("lists its octets from the most significant") {
            HOST.toIpv4Address().octets shouldContainExactly hostOctets
        }

        test("belongs to the class of its leading bits") {
            classes.forEach { (text, expected) ->
                withClue(text) { text.toIpv4Address().addressClass shouldBe expected }
            }
        }
    }

    context("arithmetic") {
        test("steps to the next address across an octet") {
            "192.0.2.255".toIpv4Address().next() shouldBe "192.0.3.0".toIpv4Address()
        }

        test("steps to the previous address across an octet") {
            "192.0.3.0".toIpv4Address().previous() shouldBe "192.0.2.255".toIpv4Address()
        }

        test("shifts by any offset in both directions") {
            DOCUMENTATION.toIpv4Address().shiftedBy(STEP) shouldBe "192.0.3.1".toIpv4Address()
            "192.0.3.1".toIpv4Address().shiftedBy(-STEP) shouldBe DOCUMENTATION.toIpv4Address()
        }

        test("has no address beyond the ends of the address space") {
            Ipv4Address.BROADCAST.next().shouldBeNull()
            Ipv4Address.UNSPECIFIED.previous().shouldBeNull()
            Ipv4Address.UNSPECIFIED.shiftedBy(Long.MAX_VALUE).shouldBeNull()
            Ipv4Address.BROADCAST.shiftedBy(Long.MIN_VALUE).shouldBeNull()
        }
    }

    context("representations") {
        test("writes its bits octet by octet") {
            HOST.toIpv4Address().toBinaryString() shouldBe "11000000.10101000.00000001.00001010"
        }

        test("writes its bits in hexadecimal") {
            HOST.toIpv4Address().toHexString() shouldBe "c0a8010a"
        }

        test("names its reverse DNS entry") {
            HOST.toIpv4Address().reverseDnsName shouldBe "10.1.168.192.in-addr.arpa"
        }

        test("maps into IPv6") {
            val mapped = HOST.toIpv4Address().toMappedIpv6()

            mapped.toString() shouldBe "::ffff:192.168.1.10"
        }
    }
})
