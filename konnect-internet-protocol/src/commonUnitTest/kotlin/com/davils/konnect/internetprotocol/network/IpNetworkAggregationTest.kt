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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly

private const val FIRST_LAN: String = "10.0.0.0/24"
private const val SIXTH_LAN: String = "10.0.5.0/24"
private const val UPPER_HALF: String = "8000::/1"

private fun ipv4(vararg networks: String): List<Ipv4Network> = networks.map(String::toIpv4Network)

private fun ipv6(vararg networks: String): List<Ipv6Network> = networks.map(String::toIpv6Network)

class IpNetworkAggregationTest : FunSpec({
    context("IPv4") {
        test("joins two halves into their supernet") {
            ipv4("10.0.0.0/25", "10.0.0.128/25").aggregate() shouldContainExactly ipv4(FIRST_LAN)
        }

        test("keeps what cannot be joined into a single block") {
            val aggregated = ipv4("10.0.2.0/24", FIRST_LAN, "10.0.1.0/24").aggregate()

            aggregated shouldContainExactly ipv4("10.0.0.0/23", "10.0.2.0/24")
        }

        test("drops networks inside of others") {
            ipv4(SIXTH_LAN, "10.0.0.0/16").aggregate() shouldContainExactly ipv4("10.0.0.0/16")
        }

        test("joins at the end of the address space") {
            val aggregated = ipv4("255.255.255.0/24", "255.255.254.0/24").aggregate()

            aggregated shouldContainExactly ipv4("255.255.254.0/23")
        }

        test("keeps networks apart that do not touch") {
            ipv4(SIXTH_LAN, FIRST_LAN).aggregate() shouldContainExactly ipv4(FIRST_LAN, SIXTH_LAN)
        }

        test("leaves nothing of nothing") {
            val nothing = emptyList<Ipv4Network>()

            nothing.aggregate().shouldBeEmpty()
        }
    }

    context("IPv6") {
        test("joins both halves into the whole address space") {
            ipv6(UPPER_HALF, "::/1").aggregate() shouldContainExactly ipv6("::/0")
        }

        test("joins a network at the end of the address space into the one around it") {
            ipv6(UPPER_HALF, "ffff::/16").aggregate() shouldContainExactly ipv6(UPPER_HALF)
        }

        test("joins neighbouring networks that are not aligned to one block") {
            val aggregated = ipv6("2001:db8:1::/48", "2001:db8:2::/48").aggregate()

            aggregated shouldContainExactly ipv6("2001:db8:1::/48", "2001:db8:2::/48")
        }
    }
})
