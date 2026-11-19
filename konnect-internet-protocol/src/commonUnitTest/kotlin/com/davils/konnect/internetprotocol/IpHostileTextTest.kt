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

import com.davils.konnect.core.Limits
import com.davils.konnect.internetprotocol.address.toIpAddress
import com.davils.konnect.internetprotocol.address.toIpv4Address
import com.davils.konnect.internetprotocol.address.toIpv6Address
import com.davils.konnect.internetprotocol.mask.toIpv4Netmask
import com.davils.konnect.internetprotocol.mask.toIpv6Netmask
import com.davils.konnect.internetprotocol.range.toIpv4Range
import com.davils.konnect.internetprotocol.range.toIpv6Range
import com.davils.konnect.internetprotocol.subnet.toIpv4Interface
import com.davils.konnect.internetprotocol.subnet.toIpv4Network
import com.davils.konnect.internetprotocol.subnet.toIpv6Interface
import com.davils.konnect.internetprotocol.subnet.toIpv6Network
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val TOO_LONG: String = "is longer than 128 characters"

private val parsers: Map<String, (String) -> Any> = mapOf(
    "address" to String::toIpAddress,
    "IPv4 address" to String::toIpv4Address,
    "IPv6 address" to String::toIpv6Address,
    "IPv4 netmask" to String::toIpv4Netmask,
    "IPv6 netmask" to String::toIpv6Netmask,
    "IPv4 range" to String::toIpv4Range,
    "IPv6 range" to String::toIpv6Range,
    "IPv4 network" to String::toIpv4Network,
    "IPv6 network" to String::toIpv6Network,
    "IPv4 interface" to String::toIpv4Interface,
    "IPv6 interface" to String::toIpv6Interface,
)

class IpHostileTextTest : FunSpec({
    context("every parser") {
        test("never carries the input into a message") {
            parsers.forEach { (name, parse) ->
                withClue(name) {
                    checkAll(CASES, hostileText) { text ->
                        val failure = runCatching { parse(text) }.exceptionOrNull()

                        failure?.message.shouldBePlainAscii()
                    }
                }
            }
        }

        test("refuses a text above the limit before it reads it") {
            val longIpv4 = "1." + "1".repeat(Limits.MAX_TEXT_LENGTH)
            val longRange = "192.0.2.1" + " ".repeat(Limits.MAX_TEXT_LENGTH) + "-192.0.2.9"

            parsers.forEach { (name, parse) ->
                withClue(name) {
                    val failure = shouldThrow<IllegalArgumentException> { parse(longIpv4) }

                    failure.message.orEmpty() shouldContain "is longer than"
                }
            }
            shouldThrow<IllegalArgumentException> { longRange.toIpv4Range() }.message.orEmpty() shouldEndWith TOO_LONG
        }
    }
})
