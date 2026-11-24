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

package com.davils.konnect.testing

import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.network.Ipv4Network
import com.davils.konnect.internetprotocol.network.Ipv6Network

private const val SITE_PREFIX_LENGTH: Int = 48
private const val SITE_TRAILING_GROUPS: Int = 5

private val ipv4Documentation: List<Ipv4Network> =
    listOf("192.0.2.0/24", "198.51.100.0/24", "203.0.113.0/24").map(Ipv4Network::parse)

private val ipv6Documentation: List<Int> = listOf(0x2001, 0xdb8)

/**
 * Networks for tests: the documentation networks of RFC 5737 and networks inside the documentation block
 * `2001:db8::/32` of RFC 3849, which are never routed on the public internet.
 *
 * The same arguments always give the same networks, so a test that names them is repeatable.
 *
 * @since 1.0.0
 */
public object TestNetworks {
    /**
     * Returns one of the three IPv4 documentation networks: `192.0.2.0/24` (TEST-NET-1) for `0`, `198.51.100.0/24`
     * (TEST-NET-2) for `1` and `203.0.113.0/24` (TEST-NET-3) for `2`.
     *
     * @param index the number of the network, from `0` to `2`.
     * @return a documentation network with the prefix length `24`.
     * @throws IllegalArgumentException if [index] is outside `0..2`.
     * @since 1.0.0
     */
    public fun ipv4(index: Int = 0): Ipv4Network {
        require(index in ipv4Documentation.indices) {
            "There are ${ipv4Documentation.size} IPv4 documentation networks, but index $index was asked for"
        }
        return ipv4Documentation[index]
    }

    /**
     * Returns the IPv6 site network `2001:db8:<site>::/48` inside the documentation block.
     *
     * @param site the third group of the network, from `0` to `65535`.
     * @return a network with the prefix length `48` in `2001:db8::/32`.
     * @throws IllegalArgumentException if [site] is outside `0..65535`.
     * @since 1.0.0
     */
    public fun ipv6(site: Int = 0): Ipv6Network {
        val address = Ipv6Address(ipv6Documentation + site + List(SITE_TRAILING_GROUPS) { 0 })
        return Ipv6Network(address, SITE_PREFIX_LENGTH)
    }
}
