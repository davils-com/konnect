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

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.address.AddressCategory
import com.davils.konnect.internetprotocol.range.toIpv4Range
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val HOSTS_24: Long = 254L
private const val HOSTS_20: Long = 4094L
private const val HOST_BITS_64: Int = 64

class IpcalcScenarioTest : FunSpec({
    context("ipcalc for an IPv4 interface") {
        test("describes a host in a /24") {
            IpcalcReport.describe("192.168.1.10/24".toIpv4Interface()) shouldBe IpcalcReport(
                address = "192.168.1.10",
                binary = "11000000.10101000.00000001.00001010",
                netmask = "255.255.255.0 = 24",
                wildcard = "0.0.0.255",
                network = "192.168.1.0/24",
                broadcast = "192.168.1.255",
                firstHost = "192.168.1.1",
                lastHost = "192.168.1.254",
                hosts = HOSTS_24,
                addressClass = "C",
                reverseDns = "10.1.168.192.in-addr.arpa",
                category = AddressCategory.Private,
            )
        }

        test("describes a point-to-point link") {
            IpcalcReport.describe("10.0.0.1/31".toIpv4Interface()) shouldBe IpcalcReport(
                address = "10.0.0.1",
                binary = "00001010.00000000.00000000.00000001",
                netmask = "255.255.255.254 = 31",
                wildcard = "0.0.0.1",
                network = "10.0.0.0/31",
                broadcast = null,
                firstHost = "10.0.0.0",
                lastHost = "10.0.0.1",
                hosts = 2L,
                addressClass = "A",
                reverseDns = "1.0.0.10.in-addr.arpa",
                category = AddressCategory.Private,
            )
        }

        test("describes a host given with a dotted netmask") {
            IpcalcReport.describe("172.16.5.4/255.255.240.0".toIpv4Interface()) shouldBe IpcalcReport(
                address = "172.16.5.4",
                binary = "10101100.00010000.00000101.00000100",
                netmask = "255.255.240.0 = 20",
                wildcard = "0.0.15.255",
                network = "172.16.0.0/20",
                broadcast = "172.16.15.255",
                firstHost = "172.16.0.1",
                lastHost = "172.16.15.254",
                hosts = HOSTS_20,
                addressClass = "B",
                reverseDns = "4.5.16.172.in-addr.arpa",
                category = AddressCategory.Private,
            )
        }
    }

    context("ipcalc for an IPv6 interface") {
        test("describes a host in a /64") {
            val host = "2001:db8::1/64".toIpv6Interface()
            val network = host.network

            network.toString() shouldBe "2001:db8::/64"
            network.netmask.toString() shouldBe "ffff:ffff:ffff:ffff::"
            network.wildcard.toString() shouldBe "::ffff:ffff:ffff:ffff"
            network.firstHost.toString() shouldBe "2001:db8::"
            network.lastHost.toString() shouldBe "2001:db8::ffff:ffff:ffff:ffff"
            network.hostCount shouldBe AddressCount.ofPowerOfTwo(HOST_BITS_64)
            host.address.toFullString() shouldBe "2001:0db8:0000:0000:0000:0000:0000:0001"
            host.address.category shouldBe AddressCategory.Documentation
        }
    }

    context("ipcalc for a range") {
        test("deaggregates a range into the fewest networks") {
            val networks = "10.0.0.1-10.0.0.37".toIpv4Range().toNetworks()

            networks.map(IpNetwork::toString) shouldContainExactly listOf(
                "10.0.0.1/32",
                "10.0.0.2/31",
                "10.0.0.4/30",
                "10.0.0.8/29",
                "10.0.0.16/28",
                "10.0.0.32/30",
                "10.0.0.36/31",
            )
        }
    }
})
