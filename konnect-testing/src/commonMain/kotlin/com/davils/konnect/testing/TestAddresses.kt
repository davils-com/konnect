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

import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.Ipv6Address

private val ipv6Documentation: List<Int> = listOf(0x2001, 0xdb8, 0, 0, 0, 0, 0)

/**
 * Addresses for tests: fixed addresses of the documentation blocks `192.0.2.0/24` of RFC 5737 and `2001:db8::/32` of
 * RFC 3849, which are never routed on the public internet.
 *
 * The same arguments always give the same addresses, so a test that names them is repeatable. Every address of this
 * object is of the category
 * [AddressCategory.Documentation][com.davils.konnect.internetprotocol.address.AddressCategory.Documentation].
 *
 * @since 1.0.0
 */
public object TestAddresses {
    /**
     * Returns the IPv4 address `192.0.2.<host>` of the documentation block TEST-NET-1.
     *
     * @param host the last octet of the address, from `0` to `255`.
     * @return an address in `192.0.2.0/24`.
     * @throws IllegalArgumentException if [host] is outside `0..255`.
     * @since 1.0.0
     */
    public fun ipv4(host: Int = 1): Ipv4Address = Ipv4Address(first = 192, second = 0, third = 2, fourth = host)

    /**
     * Returns the IPv6 address `2001:db8::<host>` of the documentation block, whose last group is [host].
     *
     * @param host the last group of the address, from `0` to `65535`.
     * @return an address in `2001:db8::/32`.
     * @throws IllegalArgumentException if [host] is outside `0..65535`.
     * @since 1.0.0
     */
    public fun ipv6(host: Int = 1): Ipv6Address = Ipv6Address(ipv6Documentation + host)
}
