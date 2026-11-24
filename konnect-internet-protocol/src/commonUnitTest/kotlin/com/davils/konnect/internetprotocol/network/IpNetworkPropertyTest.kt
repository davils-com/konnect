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
import com.davils.konnect.internetprotocol.address.Ipv4Address
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val OCTET_MAX: Int = 255
private const val OCTETS: Int = 4
private const val SHORTEST: Int = 8
private const val LONGEST: Int = 28
private const val MOST_EXTRA_BITS: Int = 4

private val networks: Arb<Ipv4Network> = arbitrary {
    val octets = Arb.list(Arb.int(0..OCTET_MAX), OCTETS..OCTETS).bind()
    val address = Ipv4Address(first = octets[0], second = octets[1], third = octets[2], fourth = octets[3])
    val host = Ipv4Interface(address, Arb.int(SHORTEST..LONGEST).bind())
    host.network
}

class IpNetworkPropertyTest : FunSpec({
    context("subnets") {
        test("cover their network without a gap and join back into it") {
            checkAll(CASES, networks, Arb.int(0..MOST_EXTRA_BITS)) { network, extraBits ->
                val subnets = network.subnets(network.prefixLength + extraBits).toList()

                val expectedCount = 1 shl extraBits

                subnets.size shouldBe expectedCount
                subnets.first().firstAddress shouldBe network.firstAddress
                subnets.last().lastAddress shouldBe network.lastAddress
                subnets.aggregate() shouldBe listOf(network)
            }
        }

        test("count as many addresses as their network") {
            checkAll(CASES, networks, Arb.int(0..MOST_EXTRA_BITS)) { network, extraBits ->
                val subnets = network.subnets(network.prefixLength + extraBits)

                subnets.fold(AddressCount.ZERO) { total, subnet -> total + subnet.addressCount } shouldBe
                    network.addressCount
            }
        }
    }

    context("masks") {
        test("the netmask and the wildcard together set every bit exactly once") {
            checkAll(CASES, networks) { network ->
                val netmask = network.netmask.address.toByteArray()
                val wildcard = network.wildcard.toByteArray()

                netmask.indices.map { index -> netmask[index].toInt() xor wildcard[index].toInt() } shouldBe
                    List(OCTETS) { -1 }
            }
        }
    }
})
