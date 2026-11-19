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

private const val IPV6_GROUP_BITS: Int = 16
private const val NIBBLE_BITS: Int = 4
private const val BINARY_RADIX: Int = 2
private const val HEX_RADIX: Int = 16
private const val BYTE_DIGITS: Int = 2
private const val IPV4_ZONE: String = "in-addr.arpa"
private const val IPV6_ZONE: String = "ip6.arpa"
private const val LABEL_SEPARATOR: String = "."

internal fun dottedQuad(bits: Bits128): String {
    val octets = bits.groupsOf(IpVersion.V4, Byte.SIZE_BITS)
    return octets.joinToString(LABEL_SEPARATOR)
}

internal fun addressText(bits: Bits128, version: IpVersion): String = when (version) {
    IpVersion.V4 -> dottedQuad(bits)
    IpVersion.V6 -> canonicalIpv6(bits)
}

internal fun binaryText(bits: Bits128, version: IpVersion): String {
    val groupBits = when (version) {
        IpVersion.V4 -> Byte.SIZE_BITS
        IpVersion.V6 -> IPV6_GROUP_BITS
    }
    val separator = when (version) {
        IpVersion.V4 -> LABEL_SEPARATOR
        IpVersion.V6 -> ":"
    }
    val groups = bits.groupsOf(version, groupBits)
    return groups.joinToString(separator) { group -> group.toString(BINARY_RADIX).padStart(groupBits, '0') }
}

internal fun hexText(bits: Bits128, version: IpVersion): String {
    val bytes = bits.groupsOf(version, Byte.SIZE_BITS)
    return bytes.joinToString("") { byte -> byte.toString(HEX_RADIX).padStart(BYTE_DIGITS, '0') }
}

internal fun reverseDnsName(bits: Bits128, version: IpVersion): String = when (version) {
    IpVersion.V4 -> reversed(bits.groupsOf(version, Byte.SIZE_BITS), radix = 10, zone = IPV4_ZONE)
    IpVersion.V6 -> reversed(bits.groupsOf(version, NIBBLE_BITS), radix = HEX_RADIX, zone = IPV6_ZONE)
}

private fun reversed(labels: List<Int>, radix: Int, zone: String): String {
    val written = labels.asReversed().map { label -> label.toString(radix) }
    return (written + zone).joinToString(LABEL_SEPARATOR)
}
