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

package com.davils.konnect.internetprotocol.range

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.network.aggregate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val OCTET_MAX: Int = 255
private const val WIDEST: Long = 100_000L

private val ranges: Arb<Ipv4Range> = arbitrary {
    val first = Ipv4Address(
        first = Arb.int(0..OCTET_MAX).bind(),
        second = Arb.int(0..OCTET_MAX).bind(),
        third = Arb.int(0..OCTET_MAX).bind(),
        fourth = Arb.int(0..OCTET_MAX).bind(),
    )
    val last = first.shiftedBy(Arb.long(0L..WIDEST).bind()) ?: Ipv4Address.BROADCAST
    Ipv4Range(first = first, last = last)
}

class IpRangePropertyTest : FunSpec({
    context("networks of a range") {
        test("cover the range without a gap or an overlap") {
            checkAll(CASES, ranges) { range ->
                val networks = range.toNetworks()

                networks.first().firstAddress shouldBe range.first
                networks.last().lastAddress shouldBe range.last
                networks.fold(AddressCount.ZERO) { total, network -> total + network.addressCount } shouldBe range.size
            }
        }

        test("are already as few as possible") {
            checkAll(CASES, ranges) { range ->
                val networks = range.toNetworks()

                networks.aggregate() shouldBe networks
            }
        }
    }
})
