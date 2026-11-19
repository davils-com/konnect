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

import com.davils.konnect.core.Limits
import com.davils.konnect.internetprotocol.address.bits.Bits128

internal const val OCTET_SEPARATOR: Char = '.'
internal const val EMPTY_REASON: String = "is empty"
internal const val TEXT_TOO_LONG_REASON: String = "is longer than ${Limits.MAX_TEXT_LENGTH} characters"

private const val OCTETS: Int = 4
private const val OCTET_MAX: Int = 255
private const val OCTET_DIGITS: Int = 3
private const val SHAPE_REASON: String = "must be four dot-separated numbers, such as 192.0.2.1"
private const val DIGIT_REASON: String = "has a part that is not a decimal number"
private const val LEADING_ZERO_REASON: String = "has a number with a leading zero"
private const val RANGE_REASON: String = "has a number above $OCTET_MAX"

private val decimalDigits: CharRange = '0'..'9'

internal fun readIpv4(text: String): AddressReading {
    val problem = lengthProblem(text) ?: ipv4Problem(text)
    if (problem != null) return AddressReading.Refused(problem)
    val octets = text.split(OCTET_SEPARATOR)
    return AddressReading.Read(Bits128.ofLow(ipv4Value(octets)))
}

internal fun lengthProblem(text: String): String? = TEXT_TOO_LONG_REASON.takeIf { text.length > Limits.MAX_TEXT_LENGTH }

private fun ipv4Problem(text: String): String? {
    val octets = text.split(OCTET_SEPARATOR)
    return shapeProblem(text, octets) ?: octets.firstNotNullOfOrNull(::octetProblem)
}

internal fun ipv4Value(octets: List<String>): ULong =
    octets.fold(0uL) { value, octet -> value shl Byte.SIZE_BITS or octet.toULong() }

private fun shapeProblem(text: String, octets: List<String>): String? {
    if (text.isEmpty()) return EMPTY_REASON
    return SHAPE_REASON.takeIf { octets.size != OCTETS }
}

private fun octetProblem(octet: String): String? {
    if (octet.isEmpty() || !octet.all { digit -> digit in decimalDigits }) return DIGIT_REASON
    return LEADING_ZERO_REASON.takeIf { octet.length > 1 && octet.first() == '0' }
        ?: RANGE_REASON.takeIf { octet.length > OCTET_DIGITS || octet.toInt() > OCTET_MAX }
}
