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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val LAN: String = "192.168.1.0/24"
private const val DOCUMENTATION: String = "2001:db8::/32"
private const val PREFIX_16: Int = 16
private const val PREFIX_23: Int = 23
private const val PREFIX_24: Int = 24
private const val PREFIX_25: Int = 25
private const val PREFIX_26: Int = 26
private const val PREFIX_33: Int = 33
private const val THREE: Int = 3
private const val FOUR: Int = 4

private val quarters: List<String> =
    listOf("192.168.1.0/26", "192.168.1.64/26", "192.168.1.128/26", "192.168.1.192/26")

class IpNetworkSubnettingTest : FunSpec({
    context("subnets") {
        test("divide a network into every subnet of a longer prefix length") {
            val subnets = LAN.toIpv4Network().subnets(PREFIX_26)

            subnets.map(Ipv4Network::toString).toList() shouldContainExactly quarters
        }

        test("of the same prefix length are the network itself") {
            val network = LAN.toIpv4Network()

            network.subnets(PREFIX_24).toList() shouldContainExactly listOf(network)
        }

        test("reach the end of the address space") {
            val ipv4 = "0.0.0.0/0".toIpv4Network().subnets(1)
            val ipv6 = "::/0".toIpv6Network().subnets(1)

            ipv4.map(Ipv4Network::toString).toList() shouldContainExactly listOf("0.0.0.0/1", "128.0.0.0/1")
            ipv6.map(Ipv6Network::toString).toList() shouldContainExactly listOf("::/1", "8000::/1")
        }

        test("need a prefix length between the network and the full address") {
            shouldThrow<IllegalArgumentException> { LAN.toIpv4Network().subnets(PREFIX_23) }
            shouldThrow<IllegalArgumentException> { LAN.toIpv4Network().subnets(PREFIX_33) }
        }
    }

    context("splitting") {
        test("rounds the count up to the next power of two") {
            val parts = LAN.toIpv4Network().split(THREE)

            parts?.map(Ipv4Network::toString) shouldBe quarters
        }

        test("into one part keeps the network") {
            LAN.toIpv4Network().split(1) shouldBe listOf(LAN.toIpv4Network())
        }

        test("is impossible beyond the full address") {
            "10.0.0.0/31".toIpv4Network().split(FOUR) shouldBe null
            "2001:db8::/127".toIpv6Network().split(FOUR) shouldBe null
        }

        test("splits an IPv6 network as well") {
            val parts = DOCUMENTATION.toIpv6Network().split(2)

            parts?.map(Ipv6Network::toString) shouldBe listOf("2001:db8::/33", "2001:db8:8000::/33")
        }

        test("needs at least one part") {
            shouldThrow<IllegalArgumentException> { LAN.toIpv4Network().split(0) }
            shouldThrow<IllegalArgumentException> { DOCUMENTATION.toIpv6Network().split(0) }
        }
    }

    context("supernets") {
        test("widen the network to a shorter prefix length") {
            LAN.toIpv4Network().supernet(PREFIX_16) shouldBe "192.168.0.0/16".toIpv4Network()
            DOCUMENTATION.toIpv6Network().supernet(PREFIX_16) shouldBe "2001::/16".toIpv6Network()
        }

        test("need a prefix length up to the one of the network") {
            shouldThrow<IllegalArgumentException> { LAN.toIpv4Network().supernet(PREFIX_25) }
            shouldThrow<IllegalArgumentException> { LAN.toIpv4Network().supernet(-1) }
        }
    }

    context("siblings") {
        test("are the neighbouring networks of the same prefix length") {
            LAN.toIpv4Network().nextSibling() shouldBe "192.168.2.0/24".toIpv4Network()
            LAN.toIpv4Network().previousSibling() shouldBe "192.168.0.0/24".toIpv4Network()
            DOCUMENTATION.toIpv6Network().nextSibling() shouldBe "2001:db9::/32".toIpv6Network()
            DOCUMENTATION.toIpv6Network().previousSibling() shouldBe "2001:db7::/32".toIpv6Network()
        }

        test("end at the ends of the address space") {
            "255.255.255.0/24".toIpv4Network().nextSibling() shouldBe null
            "0.0.0.0/24".toIpv4Network().previousSibling() shouldBe null
            "ffff::/16".toIpv6Network().nextSibling() shouldBe null
            "::/16".toIpv6Network().previousSibling() shouldBe null
        }
    }

    context("hosts") {
        test("leave out the network and the broadcast address") {
            val hosts = "192.168.1.0/30".toIpv4Network().hosts()

            hosts.map(Any::toString).toList() shouldContainExactly listOf("192.168.1.1", "192.168.1.2")
        }

        test("are both addresses of a point-to-point link") {
            val hosts = "10.0.0.0/31".toIpv4Network().hosts()

            hosts.map(Any::toString).toList() shouldContainExactly listOf("10.0.0.0", "10.0.0.1")
        }

        test("are the single address of a host network, even at the end of the address space") {
            val hosts = "255.255.255.255/32".toIpv4Network().hosts()

            hosts.map(Any::toString).toList() shouldContainExactly listOf("255.255.255.255")
        }

        test("end at the end of the IPv6 address space") {
            val hosts = "ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe/127".toIpv6Network().hosts()

            hosts.count() shouldBe 2
        }

        test("are every address of an IPv6 network") {
            val hosts = "2001:db8::/126".toIpv6Network().hosts()

            hosts.map(Any::toString).toList() shouldContainExactly
                listOf("2001:db8::", "2001:db8::1", "2001:db8::2", "2001:db8::3")
        }
    }
})
