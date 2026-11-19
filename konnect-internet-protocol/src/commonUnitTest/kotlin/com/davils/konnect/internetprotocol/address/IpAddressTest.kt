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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val IPV4: String = "192.0.2.1"
private const val IPV6: String = "2001:db8::1"
private const val PRIVATE: String = "10.0.0.1"
private const val LOOPBACK: String = "::1"
private const val DOUBLE_COMPRESSION: String = "1::2::3"

class IpAddressTest : FunSpec({
    context("reading either version") {
        test("reads an IPv4 address") {
            IpAddress.parse(IPV4).shouldBeInstanceOf<Ipv4Address>()
            IPV4.toIpAddress() shouldBe IPV4.toIpv4Address()
        }

        test("reads an IPv6 address") {
            IpAddress.parse(IPV6).shouldBeInstanceOf<Ipv6Address>()
            IPV6.toIpAddress() shouldBe IPV6.toIpv6Address()
        }

        test("reads either without failing") {
            IPV4.toIpAddressOrNull() shouldBe IPV4.toIpv4Address()
            IPV6.toIpAddressOrNull() shouldBe IPV6.toIpv6Address()
            "".toIpAddressOrNull().shouldBeNull()
            DOUBLE_COMPRESSION.toIpAddressOrNull().shouldBeNull()
        }

        test("reads each version without failing") {
            IPV4.toIpv4AddressOrNull() shouldBe IPV4.toIpv4Address()
            IPV6.toIpv6AddressOrNull() shouldBe IPV6.toIpv6Address()
        }

        test("says which version a refused text was read as") {
            shouldThrow<IllegalArgumentException> { IpAddress.parse(DOUBLE_COMPRESSION) }.message shouldBe
                "Not an IPv6 address: it uses '::' more than once"
            shouldThrow<IllegalArgumentException> { IpAddress.parse("1.2.3") }.message shouldBe
                "Not an IPv4 address: it must be four dot-separated numbers, such as 192.0.2.1"
        }
    }

    context("identity") {
        test("orders IPv4 before IPv6 and each by its bits") {
            val addresses = listOf(IPV6, PRIVATE, LOOPBACK, IPV4).map(String::toIpAddress)

            addresses.sorted().map(IpAddress::toString) shouldBe listOf(PRIVATE, IPV4, LOOPBACK, IPV6)
        }

        test("is never equal to the other version with the same bits") {
            val ipv4 = "0.0.0.1".toIpAddress()

            ipv4 shouldNotBe LOOPBACK.toIpAddress()
            ipv4 shouldNotBe ipv4.toString()
        }

        test("shares the hash code with an equal address") {
            IPV4.toIpAddress().hashCode() shouldBe IPV4.toIpv4Address().hashCode()
        }
    }
})
