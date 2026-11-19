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

package com.davils.konnect.internetprotocol.address.registry

import com.davils.konnect.internetprotocol.address.AddressCategory
import com.davils.konnect.internetprotocol.address.IpAddress

private const val PREFIX_SEPARATOR: Char = '/'

internal object SpecialPurposeRegistry {
    private val public: SpecialPurpose = SpecialPurpose(AddressCategory.Public, isGloballyReachable = true)

    private val entries: List<RegistryEntry> = listOf(
        entry("0.0.0.0/8", AddressCategory.ThisNetwork),
        entry("0.0.0.0/32", AddressCategory.Unspecified),
        entry("10.0.0.0/8", AddressCategory.Private),
        entry("100.64.0.0/10", AddressCategory.SharedAddressSpace),
        entry("127.0.0.0/8", AddressCategory.Loopback),
        entry("169.254.0.0/16", AddressCategory.LinkLocal),
        entry("172.16.0.0/12", AddressCategory.Private),
        entry("192.0.0.0/24", AddressCategory.ProtocolAssignment),
        entry("192.0.0.0/29", AddressCategory.ProtocolAssignment),
        entry("192.0.0.8/32", AddressCategory.ProtocolAssignment),
        entry("192.0.0.9/32", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("192.0.0.10/32", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("192.0.0.170/32", AddressCategory.ProtocolAssignment),
        entry("192.0.0.171/32", AddressCategory.ProtocolAssignment),
        entry("192.0.2.0/24", AddressCategory.Documentation),
        entry("192.31.196.0/24", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("192.52.193.0/24", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("192.88.99.0/24", AddressCategory.Reserved),
        entry("192.88.99.2/32", AddressCategory.ProtocolAssignment),
        entry("192.168.0.0/16", AddressCategory.Private),
        entry("192.175.48.0/24", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("198.18.0.0/15", AddressCategory.Benchmarking),
        entry("198.51.100.0/24", AddressCategory.Documentation),
        entry("203.0.113.0/24", AddressCategory.Documentation),
        entry("224.0.0.0/4", AddressCategory.Multicast),
        entry("233.252.0.0/24", AddressCategory.Documentation),
        entry("240.0.0.0/4", AddressCategory.Reserved),
        entry("255.255.255.255/32", AddressCategory.Broadcast),
        entry("::/3", AddressCategory.Reserved),
        entry("4000::/2", AddressCategory.Reserved),
        entry("8000::/1", AddressCategory.Reserved),
        entry("::/128", AddressCategory.Unspecified),
        entry("::1/128", AddressCategory.Loopback),
        entry("::ffff:0:0/96", AddressCategory.Ipv4Mapped),
        entry("64:ff9b::/96", AddressCategory.Translation, isGloballyReachable = true),
        entry("64:ff9b:1::/48", AddressCategory.Translation),
        entry("100::/64", AddressCategory.Discard),
        entry("100:0:0:1::/64", AddressCategory.ProtocolAssignment),
        entry("2001::/23", AddressCategory.ProtocolAssignment),
        entry("2001::/32", AddressCategory.Tunnel),
        entry("2001:1::1/128", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:1::2/128", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:1::3/128", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:2::/48", AddressCategory.Benchmarking),
        entry("2001:3::/32", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:4:112::/48", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:20::/28", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:30::/28", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("2001:db8::/32", AddressCategory.Documentation),
        entry("2002::/16", AddressCategory.Tunnel),
        entry("2620:4f:8000::/48", AddressCategory.ProtocolAssignment, isGloballyReachable = true),
        entry("3fff::/20", AddressCategory.Documentation),
        entry("5f00::/16", AddressCategory.ProtocolAssignment),
        entry("fc00::/7", AddressCategory.UniqueLocal),
        entry("fe80::/10", AddressCategory.LinkLocal),
        entry("ff00::/8", AddressCategory.Multicast),
    )

    fun purposeOf(address: IpAddress): SpecialPurpose {
        val matches = entries.filter { entry -> entry.covers(address) }
        return matches.maxByOrNull(RegistryEntry::prefixLength)?.purpose ?: public
    }

    private fun entry(block: String, category: AddressCategory, isGloballyReachable: Boolean = false): RegistryEntry {
        val address = IpAddress.parse(block.substringBefore(PREFIX_SEPARATOR))
        return RegistryEntry(
            version = address.version,
            bits = address.bits,
            prefixLength = block.substringAfter(PREFIX_SEPARATOR).toInt(),
            purpose = SpecialPurpose(category, isGloballyReachable),
        )
    }
}
