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

package com.davils.konnect.internetprotocol.range.grammar

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.grammar.AddressReading
import com.davils.konnect.internetprotocol.address.grammar.lengthProblem
import com.davils.konnect.internetprotocol.address.grammar.readAddress

private const val RANGE_SEPARATOR: Char = '-'
private const val SPACE: Char = ' '
private const val SHAPE_REASON: String = "must be one address or two joined by '-', such as 192.0.2.1-192.0.2.9"
private const val ORDER_REASON: String = "starts after it ends"

internal fun readRange(text: String, version: IpVersion): RangeReading {
    val tooLong = lengthProblem(text)
    if (tooLong != null) return RangeReading.Refused(tooLong)
    return readParts(text.split(RANGE_SEPARATOR), version)
}

private fun readParts(parts: List<String>, version: IpVersion): RangeReading {
    if (parts.size > 2) return RangeReading.Refused(SHAPE_REASON)
    val first = parts.first()
    val last = parts.last()
    return readBounds(first = readAddress(first.trim(SPACE), version), last = readAddress(last.trim(SPACE), version))
}

private fun readBounds(first: AddressReading, last: AddressReading): RangeReading {
    val problem = problemOf(first, "first") ?: problemOf(last, "last")
    if (problem != null) return RangeReading.Refused(problem)
    return ordered(first = first as AddressReading.Read, last = last as AddressReading.Read)
}

private fun ordered(first: AddressReading.Read, last: AddressReading.Read): RangeReading {
    if (first.bits > last.bits) return RangeReading.Refused(ORDER_REASON)
    return RangeReading.Read(first = first.bits, last = last.bits)
}

private fun problemOf(reading: AddressReading, bound: String): String? {
    val refused = reading as? AddressReading.Refused
    return refused?.let { "has a $bound address that ${it.reason}" }
}
