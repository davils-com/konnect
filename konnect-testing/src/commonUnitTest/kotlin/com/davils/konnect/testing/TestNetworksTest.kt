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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SITE: Int = 7
private const val MISSING: Int = 3

class TestNetworksTest : FunSpec({
    context("IPv4") {
        test("hands out the three documentation networks") {
            TestNetworks.ipv4().toString() shouldBe "192.0.2.0/24"
            TestNetworks.ipv4(1).toString() shouldBe "198.51.100.0/24"
            TestNetworks.ipv4(2).toString() shouldBe "203.0.113.0/24"
        }

        test("has no fourth documentation network") {
            shouldThrow<IllegalArgumentException> { TestNetworks.ipv4(MISSING) }
            shouldThrow<IllegalArgumentException> { TestNetworks.ipv4(-1) }
        }
    }

    context("IPv6") {
        test("hands out sites of the documentation prefix") {
            TestNetworks.ipv6().toString() shouldBe "2001:db8::/48"
            TestNetworks.ipv6(SITE).toString() shouldBe "2001:db8:7::/48"
        }

        test("contain the test addresses of the first site") {
            (TestAddresses.ipv6() in TestNetworks.ipv6()) shouldBe true
            (TestAddresses.ipv4() in TestNetworks.ipv4()) shouldBe true
        }
    }
})
