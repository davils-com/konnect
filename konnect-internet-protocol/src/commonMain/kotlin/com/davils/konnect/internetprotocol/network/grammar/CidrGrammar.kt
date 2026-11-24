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

package com.davils.konnect.internetprotocol.subnet.grammar

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.hasHostBits
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.address.grammar.AddressReading
import com.davils.konnect.internetprotocol.address.grammar.OCTET_SEPARATOR
import com.davils.konnect.internetprotocol.address.grammar.lengthProblem
import com.davils.konnect.internetprotocol.address.grammar.readAddress
import com.davils.konnect.internetprotocol.address.text.addressText
import com.davils.konnect.internetprotocol.mask.reading.MaskReading
import com.davils.konnect.internetprotocol.mask.reading.readMask

private const val PREFIX_SEPARATOR: Char = '/'
private const val PREFIX_DIGITS: Int = 3

private val decimalDigits: CharRange = '0'..'9'

internal fun readCidr(text: String, version: IpVersion): CidrReading {
    val tooLong = lengthProblem(text)
    if (tooLong != null) return CidrReading.Refused(tooLong)
    val reading = readAddress(text.substringBefore(PREFIX_SEPARATOR), version)
    return when (reading) {
        is AddressReading.Read -> withPrefix(reading, text, version)
        is AddressReading.Refused -> CidrReading.Refused("has an address that ${reading.reason}")
    }
}

internal fun readNetwork(text: String, version: IpVersion): CidrReading {
    val reading = readCidr(text, version)
    if (reading !is CidrReading.Read || !hasHostBits(reading.bits, version, reading.prefixLength)) return reading
    val network = addressText(reading.bits intersect prefixMask(version, reading.prefixLength), version)
    return CidrReading.Refused(
        "has host bits set beyond its prefix length, the network is $network/${reading.prefixLength}",
    )
}

private fun withPrefix(address: AddressReading.Read, text: String, version: IpVersion): CidrReading {
    if (PREFIX_SEPARATOR !in text) return CidrReading.Read(address.bits, version.bitLength)
    val prefix = readPrefix(text.substringAfter(PREFIX_SEPARATOR), version)
    return when (prefix) {
        is MaskReading.Read -> CidrReading.Read(address.bits, prefix.prefixLength)
        is MaskReading.Refused -> CidrReading.Refused(prefix.reason)
    }
}

private fun readPrefix(text: String, version: IpVersion): MaskReading {
    if (version == IpVersion.V4 && OCTET_SEPARATOR in text) return readNetmask(text, version)
    return readPrefixLength(text, version)
}

private fun readPrefixLength(text: String, version: IpVersion): MaskReading {
    val prefixLength = text.takeIf(::isDecimal)?.toInt()
    if (prefixLength != null && prefixLength <= version.bitLength) return MaskReading.Read(prefixLength)
    return MaskReading.Refused("has a prefix length that is not a number in 0..${version.bitLength}")
}

private fun readNetmask(text: String, version: IpVersion): MaskReading {
    val reading = readMask(text, version)
    return when (reading) {
        is MaskReading.Read -> reading
        is MaskReading.Refused -> MaskReading.Refused("has a netmask that ${reading.reason}")
    }
}

private fun isDecimal(text: String): Boolean {
    val isNumber = text.length in 1..PREFIX_DIGITS && text.all { digit -> digit in decimalDigits }
    return isNumber && (text.length == 1 || text.first() != '0')
}
