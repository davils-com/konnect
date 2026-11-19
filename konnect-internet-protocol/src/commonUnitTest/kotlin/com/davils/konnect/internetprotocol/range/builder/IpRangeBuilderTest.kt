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

package com.davils.konnect.internetprotocol.range.builder

import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.konnect.internetprotocol.range.ipv4Range
import com.davils.konnect.internetprotocol.range.ipv6Range
import com.davils.konnect.internetprotocol.range.toIpv4Range
import com.davils.konnect.internetprotocol.range.toIpv6Range
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class IpRangeBuilderTest : FunSpec({
    context("ranges") {
        test("builds an IPv4 range from its ends") {
            val range = ipv4Range {
                first = "192.0.2.10".toIpv4Address()
                last = "192.0.2.20".toIpv4Address()
            }

            range shouldBe "192.0.2.10-192.0.2.20".toIpv4Range()
        }

        test("builds an IPv6 range from its ends") {
            val range = ipv6Range {
                first = "2001:db8::10".toIpv6Address()
                last = "2001:db8::20".toIpv6Address()
            }

            range shouldBe "2001:db8::10-2001:db8::20".toIpv6Range()
        }

        test("defaults to the unspecified address alone") {
            ipv4Range {}.toString() shouldBe "0.0.0.0-0.0.0.0"
        }

        test("reports a last address before the first") {
            val failure = shouldThrow<ValidationException> {
                ipv6Range { first = Ipv6Address.LOOPBACK }
            }

            failure.report.subject shouldBe "range"
            failure.report.errors.map { it.path.toString() } shouldContainExactly listOf("last")
            failure.report.errors.map { it.code } shouldContainExactly listOf(IpViolationCodes.INVALID_RANGE)
        }
    }
})
