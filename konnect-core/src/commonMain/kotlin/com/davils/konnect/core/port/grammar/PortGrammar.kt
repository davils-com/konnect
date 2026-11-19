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

package com.davils.konnect.core.port.grammar

import com.davils.konnect.core.Limits

internal const val PORT_MAX: Int = 65_535

private const val PORT_DIGITS: Int = 5
private const val RANGE_SEPARATOR: Char = '-'
private const val SPACE: Char = ' '
private const val EMPTY_REASON: String = "is empty"
private const val TOO_LONG_REASON: String = "is longer than ${Limits.MAX_TEXT_LENGTH} characters"
private const val DIGIT_REASON: String = "is not a decimal number"
private const val LEADING_ZERO_REASON: String = "has a leading zero"
private const val RANGE_REASON: String = "is above $PORT_MAX"
private const val SHAPE_REASON: String = "must be one port or two joined by '-', such as 8000-8080"
private const val ORDER_REASON: String = "starts after it ends"

private val decimalDigits: CharRange = '0'..'9'

internal fun readPort(text: String): PortReading {
    val problem = shapeProblem(text) ?: valueProblem(text)
    if (problem != null) return PortReading.Refused(problem)
    return PortReading.Read(text.toInt())
}

internal fun readPortRange(text: String): PortRangeReading {
    if (text.length > Limits.MAX_TEXT_LENGTH) return PortRangeReading.Refused(TOO_LONG_REASON)
    return readParts(text.split(RANGE_SEPARATOR))
}

private fun readParts(parts: List<String>): PortRangeReading {
    if (parts.size > 2) return PortRangeReading.Refused(SHAPE_REASON)
    val first = parts.first()
    val last = parts.last()
    return readBounds(first = readPort(first.trim(SPACE)), last = readPort(last.trim(SPACE)))
}

private fun readBounds(first: PortReading, last: PortReading): PortRangeReading {
    val problem = problemOf(first, "first") ?: problemOf(last, "last")
    if (problem != null) return PortRangeReading.Refused(problem)
    return ordered(first = first as PortReading.Read, last = last as PortReading.Read)
}

private fun ordered(first: PortReading.Read, last: PortReading.Read): PortRangeReading {
    if (first.value > last.value) return PortRangeReading.Refused(ORDER_REASON)
    return PortRangeReading.Read(first = first.value, last = last.value)
}

private fun problemOf(reading: PortReading, bound: String): String? {
    val refused = reading as? PortReading.Refused
    return refused?.let { "has a $bound port that ${it.reason}" }
}

private fun shapeProblem(text: String): String? = EMPTY_REASON.takeIf { text.isEmpty() }
    ?: TOO_LONG_REASON.takeIf { text.length > Limits.MAX_TEXT_LENGTH }
    ?: DIGIT_REASON.takeIf { !text.all { digit -> digit in decimalDigits } }

private fun valueProblem(text: String): String? = LEADING_ZERO_REASON.takeIf { text.length > 1 && text.first() == '0' }
    ?: RANGE_REASON.takeIf { text.length > PORT_DIGITS || text.toInt() > PORT_MAX }
