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

package com.davils.konnect.internetprotocol.mask.reading

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.address.grammar.AddressReading
import com.davils.konnect.internetprotocol.address.grammar.readAddress

private const val NOT_CONTIGUOUS_REASON: String = "has a one after a zero, so its ones are not contiguous"

internal fun readMask(text: String, version: IpVersion): MaskReading {
    val reading = readAddress(text, version)
    return when (reading) {
        is AddressReading.Read -> maskOf(reading.bits, version)
        is AddressReading.Refused -> MaskReading.Refused(reading.reason)
    }
}

internal fun prefixLengthOf(bits: Bits128, version: IpVersion): Int? {
    val prefixLength = version.bitLength - bits.trailingZeros.coerceAtMost(version.bitLength)
    return prefixLength.takeIf { prefixMask(version, prefixLength) == bits }
}

private fun maskOf(bits: Bits128, version: IpVersion): MaskReading {
    val prefixLength = prefixLengthOf(bits, version) ?: return MaskReading.Refused(NOT_CONTIGUOUS_REASON)
    return MaskReading.Read(prefixLength)
}
