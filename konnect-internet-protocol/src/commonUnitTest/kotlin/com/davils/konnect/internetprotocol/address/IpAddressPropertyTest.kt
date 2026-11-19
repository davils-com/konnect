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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val OCTET_MAX: Int = 255
private const val GROUPS: Int = 8

private val ipv4Addresses: Arb<Ipv4Address> = arbitrary {
    val octets = Arb.list(Arb.int(0..OCTET_MAX), GROUPS / 2..GROUPS / 2).bind()
    Ipv4Address(first = octets[0], second = octets[1], third = octets[2], fourth = octets[3])
}

private val ipv6Addresses: Arb<Ipv6Address> = arbitrary {
    val groups = Arb.list(Arb.element(0, 0, 0, 1, 0xdb8, 0xffff), GROUPS..GROUPS).bind()
    Ipv6Address(groups)
}

class IpAddressPropertyTest : FunSpec({
    context("the text form") {
        test("every IPv4 address reads back from its own text form") {
            checkAll(CASES, ipv4Addresses) { address -> address.toString().toIpv4Address() shouldBe address }
        }

        test("every IPv6 address reads back from its canonical and its full text form") {
            checkAll(CASES, ipv6Addresses) { address ->
                address.toString().toIpv6Address() shouldBe address
                address.toFullString().toIpv6Address() shouldBe address
            }
        }
    }

    context("the byte form") {
        test("every address reads back from its bytes") {
            checkAll(CASES, ipv4Addresses, ipv6Addresses) { ipv4, ipv6 ->
                Ipv4Address.fromByteArray(ipv4.toByteArray()) shouldBe ipv4
                Ipv6Address.fromByteArray(ipv6.toByteArray()) shouldBe ipv6
            }
        }
    }

    context("arithmetic") {
        test("stepping forward and back returns to the same address") {
            checkAll(CASES, ipv4Addresses, ipv6Addresses) { ipv4, ipv6 ->
                (ipv4.next()?.previous() ?: ipv4) shouldBe ipv4
                (ipv6.next()?.previous() ?: ipv6) shouldBe ipv6
            }
        }
    }
})
