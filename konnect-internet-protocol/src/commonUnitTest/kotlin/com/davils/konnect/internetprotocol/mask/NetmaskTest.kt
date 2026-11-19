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

package com.davils.konnect.internetprotocol.mask

import com.davils.konnect.internetprotocol.IpVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val CLASS_C: String = "255.255.255.0"
private const val SUBNET_64: String = "ffff:ffff:ffff:ffff::"
private const val PREFIX_24: Int = 24
private const val PREFIX_64: Int = 64
private const val IPV4_BITS: Int = 32
private const val IPV6_BITS: Int = 128

private val ipv4Masks: Map<String, Int> = mapOf(
    "0.0.0.0" to 0,
    "128.0.0.0" to 1,
    "255.0.0.0" to 8,
    "255.255.240.0" to 20,
    CLASS_C to PREFIX_24,
    "255.255.255.252" to 30,
    "255.255.255.254" to 31,
    "255.255.255.255" to IPV4_BITS,
)

class NetmaskTest : FunSpec({
    context("IPv4") {
        test("reads every contiguous mask with its prefix length") {
            ipv4Masks.forEach { (text, prefixLength) ->
                withClue(text) {
                    val mask = text.toIpv4Netmask()

                    mask.prefixLength shouldBe prefixLength
                    mask shouldBe Ipv4Netmask.ofPrefix(prefixLength)
                    mask.toString() shouldBe text
                }
            }
        }

        test("gives the mask and the wildcard as addresses") {
            val mask = Ipv4Netmask.ofPrefix(PREFIX_24)

            mask.address.toString() shouldBe CLASS_C
            mask.wildcard.toString() shouldBe "0.0.0.255"
            mask.hostBits shouldBe Byte.SIZE_BITS
            mask.version shouldBe IpVersion.V4
        }

        test("refuses a mask whose ones are not contiguous") {
            val failure = shouldThrow<IllegalArgumentException> { Ipv4Netmask.parse("255.0.255.0") }

            failure.message shouldBe "Not an IPv4 netmask: it has a one after a zero, so its ones are not contiguous"
            "255.255.255.1".toIpv4NetmaskOrNull().shouldBeNull()
        }

        test("refuses a text that is no address") {
            val failure = shouldThrow<IllegalArgumentException> { "255.255.0".toIpv4Netmask() }

            failure.message shouldBe "Not an IPv4 netmask: it must be four dot-separated numbers, such as 192.0.2.1"
        }

        test("reads a valid mask without failing") {
            CLASS_C.toIpv4NetmaskOrNull() shouldBe Ipv4Netmask.ofPrefix(PREFIX_24)
        }

        test("refuses a prefix length beyond the address") {
            shouldThrow<IllegalArgumentException> { Ipv4Netmask.ofPrefix(IPV4_BITS + 1) }
            shouldThrow<IllegalArgumentException> { Ipv4Netmask.ofPrefix(-1) }
        }
    }

    context("IPv6") {
        test("reads a contiguous mask with its prefix length") {
            val mask = SUBNET_64.toIpv6Netmask()

            mask.prefixLength shouldBe PREFIX_64
            mask.toString() shouldBe SUBNET_64
            mask.wildcard.toString() shouldBe "::ffff:ffff:ffff:ffff"
            mask.hostBits shouldBe PREFIX_64
        }

        test("reads the empty and the full mask") {
            "::".toIpv6Netmask().prefixLength shouldBe 0
            Ipv6Netmask.ofPrefix(IPV6_BITS).address.toString() shouldBe "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"
        }

        test("refuses a mask whose ones are not contiguous or a text that is no address") {
            "ffff::ffff".toIpv6NetmaskOrNull().shouldBeNull()
            shouldThrow<IllegalArgumentException> { Ipv6Netmask.parse("ffff::ffff") }
            "1::2::3".toIpv6NetmaskOrNull().shouldBeNull()
        }

        test("reads a valid mask without failing") {
            Ipv6Netmask.parseOrNull(SUBNET_64) shouldBe Ipv6Netmask.ofPrefix(PREFIX_64)
        }

        test("refuses a prefix length beyond the address") {
            shouldThrow<IllegalArgumentException> { Ipv6Netmask.ofPrefix(IPV6_BITS + 1) }
            shouldThrow<IllegalArgumentException> { Ipv6Netmask.ofPrefix(-1) }
        }
    }

    context("identity") {
        test("masks of different versions are different") {
            Ipv4Netmask.ofPrefix(PREFIX_24) shouldNotBe Ipv6Netmask.ofPrefix(PREFIX_24)
            Ipv4Netmask.ofPrefix(PREFIX_24) shouldNotBe CLASS_C
            Ipv4Netmask.ofPrefix(PREFIX_24) shouldNotBe Ipv4Netmask.ofPrefix(PREFIX_24 + 1)
        }

        test("equal masks share a hash code") {
            Ipv4Netmask.ofPrefix(PREFIX_24).hashCode() shouldBe CLASS_C.toIpv4Netmask().hashCode()
        }
    }
})
