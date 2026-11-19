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

import com.davils.konnect.core.Limits
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.subnet.builder.Ipv4InterfaceBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv4NetworkBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv6InterfaceBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv6NetworkBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

private const val PREFIX: Int = 24

class SubnetHardeningTest : FunSpec({
    context("split") {
        test("builds at most the limit of subnets at once") {
            val ipv4 = "0.0.0.0/0".toIpv4Network()
            val ipv6 = "::/0".toIpv6Network()

            ipv4.split(Limits.MAX_SPLIT_COUNT).orEmpty() shouldHaveSize Limits.MAX_SPLIT_COUNT
            ipv6.split(Limits.MAX_SPLIT_COUNT).orEmpty() shouldHaveSize Limits.MAX_SPLIT_COUNT
        }

        test("refuses a count above the limit before it builds a subnet") {
            shouldThrow<IllegalArgumentException> { "0.0.0.0/0".toIpv4Network().split(Int.MAX_VALUE) }
            shouldThrow<IllegalArgumentException> { "::/0".toIpv6Network().split(Limits.MAX_SPLIT_COUNT + 1) }
        }
    }

    context("builders") {
        test("refuse a change after the evaluation") {
            val ipv4Network = Ipv4NetworkBuilder()
            val ipv6Network = Ipv6NetworkBuilder()
            val ipv4Interface = Ipv4InterfaceBuilder()
            val ipv6Interface = Ipv6InterfaceBuilder()
            ipv4Network.build()
            ipv6Network.build()
            ipv4Interface.build()
            ipv6Interface.build()

            shouldThrow<IllegalStateException> { ipv4Network.prefixLength = PREFIX }
            shouldThrow<IllegalStateException> { ipv6Network.prefixLength = PREFIX }
            shouldThrow<IllegalStateException> { ipv4Interface.prefixLength = PREFIX }
            shouldThrow<IllegalStateException> { ipv6Interface.prefixLength = PREFIX }
            shouldThrow<IllegalStateException> { ipv4Network.address = Ipv4Address.parse("192.0.2.0") }
        }
    }
})
