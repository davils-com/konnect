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

import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.network.IpNetwork
import com.davils.kore.core.arguments.conversion.ConversionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

class AddressConvertersTest : FunSpec({
    context("the converter of addresses") {
        test("reads IPv4 and IPv6 addresses") {
            AddressConverters.ipAddress.convert("192.0.2.1") shouldBe IpAddress.parse("192.0.2.1")
            AddressConverters.ipAddress.convert("2001:db8::1") shouldBe IpAddress.parse("2001:db8::1")
            AddressConverters.ipAddress.typeName shouldBe "ip address"
        }

        test("refuses what is no address, without repeating it") {
            listOf("", "192.0.2.01", "fe80::1%eth0", "2001:db8::/32").forEach { raw ->
                shouldThrow<ConversionException> { AddressConverters.ipAddress.convert(raw) }
            }
            checkAll(hostileText) { raw ->
                val failure = runCatching { AddressConverters.ipAddress.convert(raw) }.exceptionOrNull()

                failure?.message.shouldBePlainAscii()
            }
        }
    }

    context("the converter of networks") {
        test("reads a network and refuses one with host bits") {
            AddressConverters.ipNetwork.convert("192.0.2.0/24") shouldBe IpNetwork.parse("192.0.2.0/24")
            AddressConverters.ipNetwork.typeName shouldBe "ip network"
            shouldThrow<ConversionException> { AddressConverters.ipNetwork.convert("192.0.2.1/24") }
        }
    }
})
