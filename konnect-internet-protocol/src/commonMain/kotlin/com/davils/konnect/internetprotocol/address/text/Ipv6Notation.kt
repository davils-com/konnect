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

package com.davils.konnect.internetprotocol.address.text

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.groupsOf

private const val GROUP_BITS: Int = 16
private const val GROUP_DIGITS: Int = 4
private const val HEX_RADIX: Int = 16
private const val MIN_RUN: Int = 2
private const val SEPARATOR: String = ":"
private const val COMPRESSION: String = "::"
private const val MAPPED_PREFIX: String = "::ffff:"
private const val MAPPED_MARK: ULong = 0xFFFFuL
private const val IPV4_BITS: Int = 32

internal fun canonicalIpv6(bits: Bits128): String {
    if (isMapped(bits)) return MAPPED_PREFIX + dottedQuad(bits)
    return compressed(bits.groupsOf(IpVersion.V6, GROUP_BITS))
}

internal fun fullIpv6(bits: Bits128): String {
    val groups = bits.groupsOf(IpVersion.V6, GROUP_BITS)
    return groups.joinToString(SEPARATOR) { group -> group.toString(HEX_RADIX).padStart(GROUP_DIGITS, '0') }
}

internal fun isMapped(bits: Bits128): Boolean = bits.high == 0uL && bits.low shr IPV4_BITS == MAPPED_MARK

private fun compressed(groups: List<Int>): String {
    val run = longestZeroRun(groups) ?: return joined(groups)
    val before = groups.subList(fromIndex = 0, toIndex = run.first)
    val after = groups.subList(fromIndex = run.last + 1, toIndex = groups.size)
    return joined(before) + COMPRESSION + joined(after)
}

private fun joined(groups: List<Int>): String = groups.joinToString(SEPARATOR) { group -> group.toString(HEX_RADIX) }

private fun longestZeroRun(groups: List<Int>): IntRange? {
    val runs = groups.indices.filter { index -> isRunStart(groups, index) }
    val ranges = runs.map { start -> start until runEnd(groups, start) }
    return ranges.filter { range -> range.count() >= MIN_RUN }.maxByOrNull(IntRange::count)
}

private fun isRunStart(groups: List<Int>, index: Int): Boolean = groups[index] == 0 && groups.getOrNull(index - 1) != 0

private fun runEnd(groups: List<Int>, start: Int): Int {
    val rest = groups.subList(fromIndex = start, toIndex = groups.size)
    return start + rest.takeWhile { group -> group == 0 }.size
}
