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

package com.davils.konnect.internetprotocol.subnet.arithmetic

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.Bits128

internal fun merged(intervals: List<Interval>): List<Interval> {
    val merged = mutableListOf<Interval>()
    intervals.sortedBy(Interval::first).forEach { interval ->
        val previous = merged.lastOrNull()
        if (previous == null || !touches(previous, interval)) {
            merged += interval
            return@forEach
        }
        merged[merged.lastIndex] = Interval(first = previous.first, last = maxOf(a = previous.last, b = interval.last))
    }
    return merged.toList()
}

internal fun cidrBlocks(interval: Interval, version: IpVersion): List<CidrBlock> {
    val first = largestBlock(interval, version)
    val blocks = generateSequence(first) { block -> followingBlock(block, interval.last, version) }
    return blocks.toList()
}

private fun followingBlock(block: CidrBlock, last: Bits128, version: IpVersion): CidrBlock? {
    val start = nextBlock(block.bits, version, block.prefixLength)?.takeIf { next -> next <= last }
    return start?.let { bits -> largestBlock(Interval(first = bits, last = last), version) }
}

private fun largestBlock(interval: Interval, version: IpVersion): CidrBlock {
    val start = interval.first
    val alignedHostBits = start.trailingZeros.coerceAtMost(version.bitLength)
    val hostBits = (alignedHostBits downTo 0).first { bits -> start union Bits128.lowOnes(bits) <= interval.last }
    return CidrBlock(start, version.bitLength - hostBits)
}

private fun touches(previous: Interval, next: Interval): Boolean {
    val following = previous.last + Bits128.ONE ?: return true
    return next.first <= following
}
