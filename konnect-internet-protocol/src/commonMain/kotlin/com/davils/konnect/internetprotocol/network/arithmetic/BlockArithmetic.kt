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

package com.davils.konnect.internetprotocol.network.arithmetic

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.allOnes
import com.davils.konnect.internetprotocol.address.bits.hostMask
import com.davils.konnect.internetprotocol.address.bits.prefixMask

internal fun nextBlock(bits: Bits128, version: IpVersion, prefixLength: Int): Bits128? {
    val next = (bits union hostMask(version, prefixLength)) + Bits128.ONE ?: return null
    return next.takeIf { block -> block <= version.allOnes }
}

internal fun previousBlock(bits: Bits128, version: IpVersion, prefixLength: Int): Bits128? {
    val previous = bits - Bits128.ONE
    return previous?.intersect(prefixMask(version, prefixLength))
}

internal fun subnetBlocks(parent: CidrBlock, version: IpVersion, prefixLength: Int): Sequence<Bits128> {
    val last = parent.bits union hostMask(version, parent.prefixLength)
    return generateSequence(parent.bits) { block ->
        nextBlock(block, version, prefixLength)?.takeIf { next -> next <= last }
    }
}

internal fun splitPrefixLength(prefixLength: Int, count: Int, version: IpVersion): Int? {
    val extraBits = Int.SIZE_BITS - (count - 1).countLeadingZeroBits()
    return (prefixLength + extraBits).takeIf { split -> split <= version.bitLength }
}
