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

package com.davils.konnect.internetprotocol.subnet

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val DOCUMENTATION: String = "2001:db8::/32"
private const val PREFIX_32: Int = 32
private const val PREFIX_64: Int = 64

class Ipv6NetworkTest : FunSpec({
    context("the values ipcalc shows") {
        test("describe a network without a broadcast address") {
            val network = DOCUMENTATION.toIpv6Network()

            network.address.toString() shouldBe "2001:db8::"
            network.netmask.toString() shouldBe "ffff:ffff::"
            network.wildcard.toString() shouldBe "::ffff:ffff:ffff:ffff:ffff:ffff"
            network.firstHost shouldBe network.firstAddress
            network.lastHost.toString() shouldBe "2001:db8:ffff:ffff:ffff:ffff:ffff:ffff"
            network.lastAddress shouldBe network.lastHost
            network.hostCount shouldBe AddressCount.ofPowerOfTwo(IpVersion.V6.bitLength - PREFIX_32)
            network.addressCount shouldBe network.hostCount
            network.version shouldBe IpVersion.V6
        }

        test("count every address of the whole address space") {
            "::/0".toIpv6Network().addressCount shouldBe AddressCount.ofPowerOfTwo(IpVersion.V6.bitLength)
        }
    }

    context("reading") {
        test("reads an address without a prefix as a single host") {
            "2001:db8::1".toIpv6Network().prefixLength shouldBe IpVersion.V6.bitLength
        }

        test("says why a text is no IPv6 network") {
            val hostBits = shouldThrow<IllegalArgumentException> { Ipv6Network.parse("2001:db8::1/64") }
            val prefix = shouldThrow<IllegalArgumentException> { Ipv6Network.parse("2001:db8::/129") }
            val netmask = shouldThrow<IllegalArgumentException> { Ipv6Network.parse("2001:db8::/255.0.0.0") }

            hostBits.message shouldBe
                "Not an IPv6 network: it has host bits set beyond its prefix length, the network is 2001:db8::/64"
            prefix.message shouldBe "Not an IPv6 network: it has a prefix length that is not a number in 0..128"
            netmask.message shouldBe "Not an IPv6 network: it has a prefix length that is not a number in 0..128"
            "2001:db8::1/64".toIpv6NetworkOrNull().shouldBeNull()
        }

        test("reads a valid text without failing") {
            Ipv6Network.parseOrNull(DOCUMENTATION) shouldBe DOCUMENTATION.toIpv6Network()
        }
    }

    context("construction") {
        test("builds a network from its address and prefix length") {
            Ipv6Network("2001:db8::".toIpv6Address(), PREFIX_32) shouldBe DOCUMENTATION.toIpv6Network()
        }

        test("reports host bits") {
            val failure = shouldThrow<ValidationException> {
                Ipv6Network("2001:db8::1".toIpv6Address(), PREFIX_64)
            }

            failure.report.errors
                .single()
                .code shouldBe IpViolationCodes.HOST_BITS_SET
            failure.report.errors
                .single()
                .path
                .toString() shouldBe "address"
        }
    }
})
