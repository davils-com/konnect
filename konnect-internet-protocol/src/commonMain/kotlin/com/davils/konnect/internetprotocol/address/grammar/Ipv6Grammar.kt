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

package com.davils.konnect.internetprotocol.address.grammar

import com.davils.konnect.internetprotocol.Limits
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.core.Limits as CoreLimits

internal const val GROUP_SEPARATOR: Char = ':'
internal const val COMPRESSION: String = "::"

private const val ZONE_SEPARATOR: Char = '%'
private const val HALF_GROUPS: Int = 4
private const val GROUP_BITS: Int = 16
private const val TOO_LONG_REASON: String = "is longer than ${Limits.MAX_IPV6_ADDRESS_LENGTH} characters"
private const val ZONE_REASON: String = "has a zone index, which is not supported"
private const val DOUBLE_COMPRESSION_REASON: String = "uses '::' more than once"

internal fun isIpv6Text(text: String): Boolean = text.length <= CoreLimits.MAX_TEXT_LENGTH && GROUP_SEPARATOR in text

internal fun readIpv6(text: String): AddressReading {
    val problem = textProblem(text) ?: Ipv6Layout.fromText(text).problem()
    if (problem != null) return AddressReading.Refused(problem)
    val layout = Ipv6Layout.fromText(text)
    val groups = layout.groups()
    val bits = Bits128(high = valueOf(groups.take(HALF_GROUPS)), low = valueOf(groups.drop(HALF_GROUPS)))
    return AddressReading.Read(bits)
}

private fun textProblem(text: String): String? = EMPTY_REASON.takeIf { text.isEmpty() }
    ?: TOO_LONG_REASON.takeIf { text.length > Limits.MAX_IPV6_ADDRESS_LENGTH }
    ?: ZONE_REASON.takeIf { ZONE_SEPARATOR in text }
    ?: DOUBLE_COMPRESSION_REASON.takeIf { text.split(COMPRESSION).size > 2 }

private fun valueOf(groups: List<Int>): ULong = groups.fold(
    0uL,
) { value, group -> value shl GROUP_BITS or group.toULong() }
