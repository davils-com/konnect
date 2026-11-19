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

import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.range.builder.Ipv4RangeBuilder
import com.davils.konnect.internetprotocol.range.builder.Ipv6RangeBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class RangeHardeningTest : FunSpec({
    context("reading") {
        test("allows spaces around the dash but no other whitespace") {
            "192.0.2.1 - 192.0.2.9".toIpv4Range().toString() shouldBe "192.0.2.1-192.0.2.9"
            "192.0.2.1\t-192.0.2.9".toIpv4RangeOrNull().shouldBeNull()
            "2001:db8::1-\n2001:db8::9".toIpv6RangeOrNull().shouldBeNull()
        }
    }

    context("builders") {
        test("refuse a change after the evaluation") {
            val ipv4 = Ipv4RangeBuilder()
            val ipv6 = Ipv6RangeBuilder()
            ipv4.build()
            ipv6.build()

            shouldThrow<IllegalStateException> { ipv4.last = Ipv4Address.parse("192.0.2.9") }
            shouldThrow<IllegalStateException> { ipv6.first = ipv6.last }
        }
    }
})
