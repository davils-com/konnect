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

package com.davils.konnect.internetprotocol.network

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val HOST: String = "192.168.1.10/24"
private const val IPV6_HOST: String = "2001:db8::1/64"
private const val PREFIX_24: Int = 24
private const val PREFIX_64: Int = 64
private const val HIGH_PREFIX: Int = 129

class IpInterfaceTest : FunSpec({
    context("IPv4") {
        test("keeps its address and finds its network") {
            val host = HOST.toIpv4Interface()

            host.address.toString() shouldBe "192.168.1.10"
            host.network shouldBe "192.168.1.0/24".toIpv4Network()
            host.netmask.toString() shouldBe "255.255.255.0"
            host.version shouldBe IpVersion.V4
            host.toString() shouldBe HOST
        }

        test("reads a prefix written as a netmask") {
            "172.16.5.4/255.255.240.0".toIpv4Interface().network.toString() shouldBe "172.16.0.0/20"
        }

        test("says why a text is no IPv4 interface") {
            val failure = shouldThrow<IllegalArgumentException> { Ipv4Interface.parse("192.168.1.10/40") }

            failure.message shouldBe "Not an IPv4 interface: it has a prefix length that is not a number in 0..32"
            "192.168.1.10/40".toIpv4InterfaceOrNull().shouldBeNull()
            Ipv4Interface.parseOrNull(HOST) shouldBe HOST.toIpv4Interface()
        }

        test("is built from its address and prefix length") {
            Ipv4Interface("192.168.1.10".toIpv4Address(), PREFIX_24) shouldBe HOST.toIpv4Interface()
        }
    }

    context("IPv6") {
        test("keeps its address and finds its network") {
            val host = IPV6_HOST.toIpv6Interface()

            host.network shouldBe "2001:db8::/64".toIpv6Network()
            host.netmask.prefixLength shouldBe PREFIX_64
            host.version shouldBe IpVersion.V6
        }

        test("says why a text is no IPv6 interface") {
            val failure = shouldThrow<IllegalArgumentException> { Ipv6Interface.parse("2001:db8::x/64") }

            failure.message shouldBe
                "Not an IPv6 interface: it has an address that has a group that is not one to four hexadecimal digits"
            "2001:db8::x/64".toIpv6InterfaceOrNull().shouldBeNull()
            Ipv6Interface.parseOrNull(IPV6_HOST) shouldBe IPV6_HOST.toIpv6Interface()
        }

        test("reports a prefix length beyond the address") {
            val failure = shouldThrow<ValidationException> {
                Ipv6Interface("2001:db8::1".toIpv6Address(), HIGH_PREFIX)
            }

            failure.report.errors
                .single()
                .code shouldBe IpViolationCodes.INVALID_PREFIX
            failure.report.errors
                .single()
                .path
                .toString() shouldBe "prefixLength"
        }

        test("is built from its address and prefix length") {
            Ipv6Interface("2001:db8::1".toIpv6Address(), PREFIX_64) shouldBe IPV6_HOST.toIpv6Interface()
        }
    }

    context("either version") {
        test("is read by the version of its address") {
            IpInterface.parse(HOST) shouldBe HOST.toIpv4Interface()
            IpInterface.parse(IPV6_HOST) shouldBe IPV6_HOST.toIpv6Interface()
            IpInterface.parseOrNull(HOST) shouldBe HOST.toIpv4Interface()
            IpInterface.parseOrNull("2001:db8::/200").shouldBeNull()
        }

        test("orders by address and then by prefix length") {
            val interfaces = listOf("10.0.0.1/16", "10.0.0.1/8", IPV6_HOST).map(IpInterface::parse)

            interfaces.sorted().map(IpInterface::toString) shouldContainExactly
                listOf("10.0.0.1/8", "10.0.0.1/16", IPV6_HOST)
        }

        test("equal interfaces share a hash code and differ from every other value") {
            HOST.toIpv4Interface().hashCode() shouldBe HOST.toIpv4Interface().hashCode()
            HOST.toIpv4Interface() shouldNotBe "192.168.1.10/25".toIpv4Interface()
            HOST.toIpv4Interface() shouldNotBe HOST
            HOST.toIpv4Interface() shouldNotBe "192.168.1.11/24".toIpv4Interface()
        }
    }
})
