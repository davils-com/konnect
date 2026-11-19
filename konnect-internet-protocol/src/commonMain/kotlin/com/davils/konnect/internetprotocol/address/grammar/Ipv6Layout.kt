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

private const val GROUPS: Int = 8
private const val GROUP_DIGITS: Int = 4
private const val GROUP_BITS: Int = 16
private const val GROUP_MASK: Int = 0xFFFF
private const val HEX_RADIX: Int = 16
private const val IPV4_GROUPS: Int = 2
private const val GROUP_REASON: String = "has a group that is not one to four hexadecimal digits"
private const val COMPRESSED_COUNT_REASON: String = "has more than seven groups beside '::'"
private const val COUNT_REASON: String = "must have eight colon-separated groups, or '::' for a run of zeros"

private val hexDigits: Set<Char> = (('0'..'9') + ('a'..'f') + ('A'..'F')).toSet()

internal class Ipv6Layout private constructor(
    private val head: List<String>,
    private val tail: List<String>,
    private val isCompressed: Boolean,
) {
    private val parts: List<String> = head + tail

    private val finalPart: String? = parts.lastOrNull().takeUnless { isCompressed && tail.isEmpty() }

    private val hasIpv4Ending: Boolean = finalPart?.contains(OCTET_SEPARATOR) == true

    fun problem(): String? {
        val hexParts = parts.dropLast(1).takeIf { hasIpv4Ending } ?: parts
        if (!hexParts.all(::isHexGroup)) return GROUP_REASON
        return ipv4Problem() ?: countProblem()
    }

    fun groups(): List<Int> {
        val leading = head.flatMap(::valuesOf)
        val trailing = tail.flatMap(::valuesOf)
        return leading + List(GROUPS - leading.size - trailing.size) { 0 } + trailing
    }

    private fun ipv4Problem(): String? {
        if (!hasIpv4Ending) return null
        val reading = readIpv4(parts.last()) as? AddressReading.Refused
        return reading?.let { refused -> "has an embedded IPv4 address that ${refused.reason}" }
    }

    private fun countProblem(): String? {
        val ipv4Extra = (IPV4_GROUPS - 1).takeIf { hasIpv4Ending } ?: 0
        val count = parts.size + ipv4Extra
        if (isCompressed) return COMPRESSED_COUNT_REASON.takeIf { count >= GROUPS }
        return COUNT_REASON.takeIf { count != GROUPS }
    }

    private fun valuesOf(part: String): List<Int> {
        if (OCTET_SEPARATOR !in part) return listOf(part.toInt(HEX_RADIX))
        val value = ipv4Value(part.split(OCTET_SEPARATOR)).toInt()
        return listOf(value ushr GROUP_BITS, value and GROUP_MASK)
    }

    companion object {
        fun fromText(text: String): Ipv6Layout = Ipv6Layout(
            head = partsOf(text.substringBefore(COMPRESSION)),
            tail = partsOf(text.substringAfter(COMPRESSION, missingDelimiterValue = "")),
            isCompressed = COMPRESSION in text,
        )

        private fun partsOf(section: String): List<String> {
            if (section.isEmpty()) return emptyList()
            return section.split(GROUP_SEPARATOR)
        }
    }
}

private fun isHexGroup(part: String): Boolean =
    part.length in 1..GROUP_DIGITS && part.all { digit -> digit in hexDigits }
