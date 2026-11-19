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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val PUBLIC_DNS: String = "8.8.8.8"
private const val LINK_LOCAL: String = "fe80::1"
private const val ALL_NODES: String = "ff02::1"

private val unreachable: (AddressCategory) -> Pair<AddressCategory, Boolean> = { category -> category to false }
private val reachable: (AddressCategory) -> Pair<AddressCategory, Boolean> = { category -> category to true }

private val ipv4Samples: Map<String, Pair<AddressCategory, Boolean>> = mapOf(
    "0.0.0.0" to unreachable(AddressCategory.Unspecified),
    "0.1.2.3" to unreachable(AddressCategory.ThisNetwork),
    "10.20.30.40" to unreachable(AddressCategory.Private),
    "100.64.0.1" to unreachable(AddressCategory.SharedAddressSpace),
    "100.128.0.1" to reachable(AddressCategory.Public),
    "127.0.0.1" to unreachable(AddressCategory.Loopback),
    "169.254.10.1" to unreachable(AddressCategory.LinkLocal),
    "172.16.0.1" to unreachable(AddressCategory.Private),
    "172.31.255.255" to unreachable(AddressCategory.Private),
    "172.32.0.1" to reachable(AddressCategory.Public),
    "192.0.0.8" to unreachable(AddressCategory.ProtocolAssignment),
    "192.0.0.9" to reachable(AddressCategory.ProtocolAssignment),
    "192.0.0.10" to reachable(AddressCategory.ProtocolAssignment),
    "192.0.2.55" to unreachable(AddressCategory.Documentation),
    "192.88.99.1" to unreachable(AddressCategory.Reserved),
    "192.168.1.10" to unreachable(AddressCategory.Private),
    "198.18.0.1" to unreachable(AddressCategory.Benchmarking),
    "198.19.255.255" to unreachable(AddressCategory.Benchmarking),
    "198.51.100.7" to unreachable(AddressCategory.Documentation),
    "203.0.113.200" to unreachable(AddressCategory.Documentation),
    "224.0.0.251" to unreachable(AddressCategory.Multicast),
    "233.252.0.1" to unreachable(AddressCategory.Documentation),
    "240.0.0.1" to unreachable(AddressCategory.Reserved),
    "255.255.255.255" to unreachable(AddressCategory.Broadcast),
    PUBLIC_DNS to reachable(AddressCategory.Public),
)

private val ipv6Samples: Map<String, Pair<AddressCategory, Boolean>> = mapOf(
    "::" to unreachable(AddressCategory.Unspecified),
    "::1" to unreachable(AddressCategory.Loopback),
    "::2" to unreachable(AddressCategory.Reserved),
    "::ffff:8.8.8.8" to unreachable(AddressCategory.Ipv4Mapped),
    "64:ff9b::808:808" to reachable(AddressCategory.Translation),
    "64:ff9b:1::1" to unreachable(AddressCategory.Translation),
    "100::1" to unreachable(AddressCategory.Discard),
    "2001::1" to unreachable(AddressCategory.ProtocolAssignment),
    "2001:1::1" to reachable(AddressCategory.ProtocolAssignment),
    "2001:1::2" to reachable(AddressCategory.ProtocolAssignment),
    "2001:1::3" to reachable(AddressCategory.ProtocolAssignment),
    "2001:2::1" to unreachable(AddressCategory.Benchmarking),
    "2001:3::1" to reachable(AddressCategory.ProtocolAssignment),
    "2001:4:112::1" to reachable(AddressCategory.ProtocolAssignment),
    "2001:20::1" to reachable(AddressCategory.ProtocolAssignment),
    "2001:30::1" to reachable(AddressCategory.ProtocolAssignment),
    "2001:db8::1" to unreachable(AddressCategory.Documentation),
    "3fff::1" to unreachable(AddressCategory.Documentation),
    "2606:4700::1111" to reachable(AddressCategory.Public),
    "2002:c000:201::1" to reachable(AddressCategory.Public),
    "4000::1" to unreachable(AddressCategory.Reserved),
    "5f00::1" to unreachable(AddressCategory.ProtocolAssignment),
    "8000::1" to unreachable(AddressCategory.Reserved),
    "fd12:3456::1" to unreachable(AddressCategory.UniqueLocal),
    LINK_LOCAL to unreachable(AddressCategory.LinkLocal),
    ALL_NODES to unreachable(AddressCategory.Multicast),
)

class AddressCategoryTest : FunSpec({
    context("the IANA special-purpose registries") {
        test("place every IPv4 sample in its block") {
            ipv4Samples.forEach { (text, expected) ->
                val address = text.toIpv4Address()

                withClue(text) { address.category to address.isPublic shouldBe expected }
            }
        }

        test("place every IPv6 sample in its block") {
            ipv6Samples.forEach { (text, expected) ->
                val address = text.toIpv6Address()

                withClue(text) { address.category to address.isPublic shouldBe expected }
            }
        }
    }

    context("shortcuts") {
        test("count RFC 1918 and unique local addresses as private") {
            listOf("10.0.0.1", "172.16.0.1", "192.168.0.1", "fd00::1").forEach { text ->
                withClue(text) { text.toIpAddress().isPrivate shouldBe true }
            }
            listOf("100.64.0.1", PUBLIC_DNS, LINK_LOCAL).forEach { text ->
                withClue(text) { text.toIpAddress().isPrivate shouldBe false }
            }
        }

        test("name loopback, link-local, multicast and unspecified addresses") {
            "127.0.0.1".toIpAddress().isLoopback shouldBe true
            LINK_LOCAL.toIpAddress().isLinkLocal shouldBe true
            ALL_NODES.toIpAddress().isMulticast shouldBe true
            "::".toIpAddress().isUnspecified shouldBe true
            val public = PUBLIC_DNS.toIpAddress()

            public.isLoopback shouldBe false
            public.isLinkLocal shouldBe false
            public.isMulticast shouldBe false
            public.isUnspecified shouldBe false
        }
    }
})
