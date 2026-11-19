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
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LAN: String = "192.168.1.0/24"
private const val PREFIX_REASON: String = "has a prefix length that is not a number in 0..32"
private const val NETWORK_ADDRESS: String = "192.168.1.0"
private const val PREFIX_24: Int = 24
private const val HOSTS_24: Long = 254L
private const val ADDRESSES_24: Long = 256L
private const val HIGH_PREFIX: Int = 33

private val refused: Map<String, String> = mapOf(
    "192.168.1.10/24" to "has host bits set beyond its prefix length, the network is 192.168.1.0/24",
    "192.168.1.0/33" to PREFIX_REASON,
    "192.168.1.0/024" to PREFIX_REASON,
    "192.168.1.0/+4" to PREFIX_REASON,
    "192.168.1.0/" to PREFIX_REASON,
    "192.168.1.0/1234" to PREFIX_REASON,
    "192.168.1.0/24/1" to PREFIX_REASON,
    "192.168.1.0/255.0.255.0" to "has a netmask that has a one after a zero, so its ones are not contiguous",
    "192.168.1/24" to "has an address that must be four dot-separated numbers, such as 192.0.2.1",
)

class Ipv4NetworkTest : FunSpec({
    context("the values ipcalc shows") {
        test("describe a network with a broadcast address") {
            val network = LAN.toIpv4Network()

            network.address.toString() shouldBe NETWORK_ADDRESS
            network.netmask.toString() shouldBe "255.255.255.0"
            network.wildcard.toString() shouldBe "0.0.0.255"
            network.prefixLength shouldBe PREFIX_24
            network.firstHost.toString() shouldBe "192.168.1.1"
            network.lastHost.toString() shouldBe "192.168.1.254"
            network.broadcast shouldBe "192.168.1.255".toIpv4Address()
            network.firstAddress shouldBe network.address
            network.lastAddress shouldBe network.broadcast
            network.hostCount shouldBe AddressCount.of(HOSTS_24.toULong())
            network.addressCount shouldBe AddressCount.of(ADDRESSES_24.toULong())
            network.version shouldBe IpVersion.V4
        }

        test("use both addresses of a point-to-point link after RFC 3021") {
            val link = "10.0.0.0/31".toIpv4Network()

            link.broadcast.shouldBeNull()
            link.firstHost.toString() shouldBe "10.0.0.0"
            link.lastHost.toString() shouldBe "10.0.0.1"
            link.hostCount shouldBe AddressCount.of(2uL)
        }

        test("treat a single address as one host") {
            val host = "10.0.0.7/32".toIpv4Network()

            host.broadcast.shouldBeNull()
            host.firstHost shouldBe host.lastHost
            host.hostCount shouldBe AddressCount.of(1uL)
        }

        test("span the whole address space with prefix length zero") {
            val everything = "0.0.0.0/0".toIpv4Network()

            everything.lastAddress.toString() shouldBe "255.255.255.255"
            everything.addressCount shouldBe AddressCount.ofPowerOfTwo(IpVersion.V4.bitLength)
        }
    }

    context("reading") {
        test("reads a network written with a netmask") {
            "172.16.0.0/255.255.240.0".toIpv4Network().toString() shouldBe "172.16.0.0/20"
        }

        test("reads an address without a prefix as a single host") {
            "192.0.2.1".toIpv4Network().prefixLength shouldBe IpVersion.V4.bitLength
        }

        test("says why a text is no IPv4 network") {
            refused.forEach { (text, reason) ->
                withClue(text) {
                    val failure = shouldThrow<IllegalArgumentException> { Ipv4Network.parse(text) }

                    failure.message shouldBe "Not an IPv4 network: it $reason"
                    text.toIpv4NetworkOrNull().shouldBeNull()
                }
            }
        }

        test("reads a valid text without failing") {
            Ipv4Network.parseOrNull(LAN) shouldBe LAN.toIpv4Network()
        }
    }

    context("construction") {
        test("builds a network from its address and prefix length") {
            Ipv4Network(NETWORK_ADDRESS.toIpv4Address(), PREFIX_24) shouldBe LAN.toIpv4Network()
        }

        test("reports host bits and a prefix length beyond the address") {
            val hostBits = shouldThrow<ValidationException> {
                Ipv4Network("192.168.1.10".toIpv4Address(), PREFIX_24)
            }
            val prefix = shouldThrow<ValidationException> {
                Ipv4Network(NETWORK_ADDRESS.toIpv4Address(), HIGH_PREFIX)
            }

            hostBits.report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.HOST_BITS_SET)
            hostBits.report.errors
                .single()
                .message shouldBe
                "has host bits set beyond the prefix length 24, the network is 192.168.1.0"
            prefix.report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.INVALID_PREFIX)
            prefix.report.errors
                .single()
                .message shouldBe "must be in 0..32, but was 33"
        }
    }

    context("containment") {
        test("contains the addresses inside of it and no other") {
            val network = LAN.toIpv4Network()

            ("192.168.1.77".toIpv4Address() in network) shouldBe true
            ("192.168.2.1".toIpv4Address() in network) shouldBe false
            ("::1".toIpv6Network().address in network) shouldBe false
        }

        test("contains the smaller networks inside of it") {
            val network = LAN.toIpv4Network()

            ("192.168.1.128/25".toIpv4Network() in network) shouldBe true
            (network in "192.168.1.128/25".toIpv4Network()) shouldBe false
            ("192.168.0.0/16".toIpv4Network() in network) shouldBe false
            ("::/0".toIpv6Network() in network) shouldBe false
        }

        test("overlaps a network that contains it or that it contains") {
            val network = LAN.toIpv4Network()

            network.overlaps("192.168.0.0/16".toIpv4Network()) shouldBe true
            network.overlaps("192.168.1.64/26".toIpv4Network()) shouldBe true
            network.overlaps("192.168.2.0/24".toIpv4Network()) shouldBe false
        }
    }

    context("identity") {
        test("orders by address and then by prefix length") {
            val networks = listOf("10.0.0.0/16", "10.0.0.0/8", "9.0.0.0/8").map(String::toIpv4Network)

            networks.sorted().map(Ipv4Network::toString) shouldContainExactly
                listOf("9.0.0.0/8", "10.0.0.0/8", "10.0.0.0/16")
        }

        test("equal networks share a hash code and differ from every other value") {
            LAN.toIpv4Network().hashCode() shouldBe LAN.toIpv4Network().hashCode()
            LAN.toIpv4Network() shouldNotBe "192.168.1.0/25".toIpv4Network()
            LAN.toIpv4Network() shouldNotBe LAN
            LAN.toIpv4Network() shouldNotBe "192.168.2.0/24".toIpv4Network()
        }

        test("reads either version") {
            IpNetwork.parse(LAN) shouldBe LAN.toIpv4Network()
            IpNetwork.parse("2001:db8::/32") shouldBe "2001:db8::/32".toIpv6Network()
            IpNetwork.parseOrNull(LAN) shouldBe LAN.toIpv4Network()
            IpNetwork.parseOrNull("2001:db8::1/32").shouldBeNull()
        }
    }
})
