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

package com.davils.konnect.internetprotocol

import com.davils.konnect.internetprotocol.address.toIpAddress
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.konnect.internetprotocol.range.toIpv4Range
import com.davils.konnect.internetprotocol.range.toIpv6Range
import com.davils.konnect.internetprotocol.subnet.toIpv4Interface
import com.davils.konnect.internetprotocol.subnet.toIpv4Network
import com.davils.konnect.internetprotocol.subnet.toIpv6Interface
import com.davils.konnect.internetprotocol.subnet.toIpv6Network
import com.davils.kore.core.validation.validate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val SUBJECT: String = "router"
private const val MANAGEMENT: String = "192.0.2.1"
private const val MANAGEMENT_V6: String = "2001:db8::1"
private const val LAN: String = "198.51.100.0/24"
private const val LAN_V6: String = "2001:db8:1::/48"

private val valid: Router = Router(
    loopback = "::1",
    management = MANAGEMENT,
    managementV6 = MANAGEMENT_V6,
    lan = LAN,
    lanV6 = LAN_V6,
    uplink = "203.0.113.2/30",
    uplinkV6 = "2001:db8:2::2/64",
    pool = "198.51.100.10-198.51.100.99",
    poolV6 = "2001:db8:1::10-2001:db8:1::99",
)

private val invalid: Router = Router(
    loopback = "127.0.0.256",
    management = "192.0.2",
    managementV6 = "2001:db8::g",
    lan = "198.51.100.1/24",
    lanV6 = "2001:db8:1::/129",
    uplink = "203.0.113.2/33",
    uplinkV6 = "2001:db8:2::2/-1",
    pool = "198.51.100.99-198.51.100.10",
    poolV6 = "2001:db8:1::99-2001:db8:1::10",
)

class IpValidationsTest : FunSpec({
    context("reading network values inside a validation") {
        test("adds nothing to the report for well-formed values and returns what it read") {
            val router = valid.copy()

            val report = router.validate(SUBJECT)

            report.violations.isEmpty() shouldBe true
            router.values shouldContainExactly listOf(
                "::1".toIpAddress(),
                MANAGEMENT.toIpv4Address(),
                MANAGEMENT_V6.toIpv6Address(),
                LAN.toIpv4Network(),
                LAN_V6.toIpv6Network(),
                "203.0.113.2/30".toIpv4Interface(),
                "2001:db8:2::2/64".toIpv6Interface(),
                "198.51.100.10-198.51.100.99".toIpv4Range(),
                "2001:db8:1::10-2001:db8:1::99".toIpv6Range(),
            )
        }

        test("reads an IPv4 address where either version is allowed") {
            val router = valid.copy(loopback = "127.0.0.1")

            router.validate(SUBJECT).violations.isEmpty() shouldBe true
            router.values.first() shouldBe "127.0.0.1".toIpAddress()
        }

        test("reports every malformed value at once, each at its path and with its code") {
            val router = invalid.copy()

            val report = router.validate(SUBJECT)

            report.errors.map { it.path.toString() } shouldContainExactly listOf(
                "loopback",
                "management",
                "managementV6",
                "lan",
                "lanV6",
                "uplink",
                "uplinkV6",
                "pool",
                "poolV6",
            )
            report.errors.map { it.code } shouldContainExactly listOf(
                IpViolationCodes.INVALID_ADDRESS,
                IpViolationCodes.INVALID_ADDRESS,
                IpViolationCodes.INVALID_ADDRESS,
                IpViolationCodes.INVALID_NETWORK,
                IpViolationCodes.INVALID_NETWORK,
                IpViolationCodes.INVALID_INTERFACE,
                IpViolationCodes.INVALID_INTERFACE,
                IpViolationCodes.INVALID_RANGE,
                IpViolationCodes.INVALID_RANGE,
            )
            router.values.all { it == null } shouldBe true
        }

        test("says why a value is refused") {
            val report = invalid.copy().validate(SUBJECT)

            report.errors.map { it.message } shouldContainExactly listOf(
                "is not an IPv4 address: it has a number above 255",
                "is not an IPv4 address: it must be four dot-separated numbers, such as 192.0.2.1",
                "is not an IPv6 address: it has a group that is not one to four hexadecimal digits",
                "is not an IPv4 network: it has host bits set beyond its prefix length, the network is 198.51.100.0/24",
                "is not an IPv6 network: it has a prefix length that is not a number in 0..128",
                "is not an IPv4 interface: it has a prefix length that is not a number in 0..32",
                "is not an IPv6 interface: it has a prefix length that is not a number in 0..128",
                "is not an IPv4 range: it starts after it ends",
                "is not an IPv6 range: it starts after it ends",
            )
        }
    }
})
