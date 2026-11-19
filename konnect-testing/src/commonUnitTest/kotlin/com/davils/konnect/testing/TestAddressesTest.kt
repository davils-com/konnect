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

package com.davils.konnect.testing

import com.davils.konnect.internetprotocol.address.AddressCategory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HOST: Int = 42
private const val TOO_HIGH: Int = 0x10000

class TestAddressesTest : FunSpec({
    context("IPv4") {
        test("hands out the hosts of TEST-NET-1") {
            TestAddresses.ipv4().toString() shouldBe "192.0.2.1"
            TestAddresses.ipv4(HOST).toString() shouldBe "192.0.2.42"
            TestAddresses.ipv4().category shouldBe AddressCategory.Documentation
        }

        test("refuses a host outside of TEST-NET-1") {
            shouldThrow<IllegalArgumentException> { TestAddresses.ipv4(-1) }
        }
    }

    context("IPv6") {
        test("hands out hosts of the documentation prefix") {
            TestAddresses.ipv6().toString() shouldBe "2001:db8::1"
            TestAddresses.ipv6(HOST).toString() shouldBe "2001:db8::2a"
            TestAddresses.ipv6().category shouldBe AddressCategory.Documentation
        }

        test("refuses a host beyond one group") {
            shouldThrow<IllegalArgumentException> { TestAddresses.ipv6(TOO_HIGH) }
        }
    }
})
