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

package com.davils.konnect.internetprotocol.subnet.builder

import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.konnect.internetprotocol.subnet.ipv4Interface
import com.davils.konnect.internetprotocol.subnet.ipv4Network
import com.davils.konnect.internetprotocol.subnet.ipv6Interface
import com.davils.konnect.internetprotocol.subnet.ipv6Network
import com.davils.konnect.internetprotocol.subnet.toIpv4Interface
import com.davils.konnect.internetprotocol.subnet.toIpv4Network
import com.davils.konnect.internetprotocol.subnet.toIpv6Interface
import com.davils.konnect.internetprotocol.subnet.toIpv6Network
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val PREFIX_24: Int = 24
private const val PREFIX_48: Int = 48
private const val TOO_LONG: Int = 40

class IpNetworkBuilderTest : FunSpec({
    context("networks") {
        test("builds an IPv4 network from its address and prefix length") {
            val network = ipv4Network {
                address = "192.168.10.0".toIpv4Address()
                prefixLength = PREFIX_24
            }

            network shouldBe "192.168.10.0/24".toIpv4Network()
        }

        test("builds an IPv6 network from its address and prefix length") {
            val network = ipv6Network {
                address = "2001:db8:1::".toIpv6Address()
                prefixLength = PREFIX_48
            }

            network shouldBe "2001:db8:1::/48".toIpv6Network()
        }

        test("defaults to the unspecified address as a single host") {
            ipv4Network {}.toString() shouldBe "0.0.0.0/32"
            ipv6Network {}.toString() shouldBe "::/128"
        }

        test("reports host bits at the address") {
            val failure = shouldThrow<ValidationException> {
                ipv4Network {
                    address = "192.168.10.1".toIpv4Address()
                    prefixLength = PREFIX_24
                }
            }

            failure.report.subject shouldBe "network"
            failure.report.errors.map { it.path.toString() } shouldContainExactly listOf("address")
            failure.report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.HOST_BITS_SET)
        }

        test("reports a prefix length beyond the address instead of host bits") {
            val builder = Ipv6NetworkBuilder()
            builder.address = Ipv6Address.LOOPBACK
            builder.prefixLength = -1

            val report = builder.validate()

            report.errors.map { it.path.toString() } shouldContainExactly listOf("prefixLength")
            report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.INVALID_PREFIX)
        }
    }

    context("interfaces") {
        test("builds an IPv4 interface with host bits") {
            val host = ipv4Interface {
                address = "192.168.10.7".toIpv4Address()
                prefixLength = PREFIX_24
            }

            host shouldBe "192.168.10.7/24".toIpv4Interface()
        }

        test("builds an IPv6 interface with host bits") {
            val host = ipv6Interface {
                address = "2001:db8:1::7".toIpv6Address()
                prefixLength = PREFIX_48
            }

            host shouldBe "2001:db8:1::7/48".toIpv6Interface()
        }

        test("reports a prefix length beyond the address") {
            val builder = Ipv4InterfaceBuilder()
            builder.address = Ipv4Address.LOOPBACK
            builder.prefixLength = TOO_LONG

            val report = builder.validate()

            report.subject shouldBe "interface"
            report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.INVALID_PREFIX)
        }

        test("defaults to the unspecified address as a single host") {
            val host = Ipv6InterfaceBuilder().build()

            host.toString() shouldBe "::/128"
        }
    }
})
