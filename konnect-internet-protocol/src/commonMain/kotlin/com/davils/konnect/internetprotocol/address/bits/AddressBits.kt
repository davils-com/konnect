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

package com.davils.konnect.internetprotocol.address.bits

import com.davils.konnect.internetprotocol.IpVersion

internal val IpVersion.allOnes: Bits128 get() = Bits128.lowOnes(bitLength)

internal val IpVersion.byteCount: Int get() = bitLength / Byte.SIZE_BITS

internal fun hostMask(version: IpVersion, prefixLength: Int): Bits128 =
    Bits128.lowOnes(version.bitLength - prefixLength)

internal fun prefixMask(version: IpVersion, prefixLength: Int): Bits128 =
    version.allOnes intersect hostMask(version, prefixLength).complement()

internal fun hasHostBits(bits: Bits128, version: IpVersion, prefixLength: Int): Boolean =
    bits intersect hostMask(version, prefixLength) != Bits128.ZERO

internal fun Bits128.groupsOf(version: IpVersion, groupBits: Int): List<Int> {
    val count = version.bitLength / groupBits
    val groupMask = Bits128.lowOnes(groupBits)
    return buildList {
        repeat(count) { index -> add((shr((count - 1 - index) * groupBits) intersect groupMask).low.toInt()) }
    }
}

internal fun Bits128.toByteArray(version: IpVersion): ByteArray {
    val bytes = groupsOf(version, Byte.SIZE_BITS)
    return ByteArray(bytes.size) { index -> bytes[index].toByte() }
}

internal fun shiftedWithin(version: IpVersion, bits: Bits128, offset: Long): Bits128? {
    val distance = Bits128.ofLow(offset.magnitude())
    if (offset < 0) return bits - distance
    return (bits + distance)?.takeIf { shifted -> shifted <= version.allOnes }
}

private fun Long.magnitude(): ULong {
    if (this >= 0) return toULong()
    return (-(this + 1)).toULong() + 1uL
}
