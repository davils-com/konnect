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

package com.davils.konnect.internetprotocol.mask

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.IpAddress

private const val HASH_FACTOR: Int = 31

/**
 * A netmask of IPv4 or IPv6: a run of ones, the prefix, followed by a run of zeros, the host bits.
 *
 * A netmask is either an [Ipv4Netmask] or an [Ipv6Netmask]. It is defined completely by its [version] and its
 * [prefixLength], so a netmask whose ones are not contiguous, such as `255.0.255.0`, cannot exist. Two netmasks are
 * equal when they have the same version and the same prefix length, and [toString] shows a netmask as an address, such
 * as `255.255.255.0` or `ffff:ffff:ffff:ffff::`. Instances are immutable.
 *
 * @since 1.0.0
 */
public sealed class Netmask {
    /**
     * The version of the internet protocol this netmask belongs to.
     *
     * @since 1.0.0
     */
    public abstract val version: IpVersion

    /**
     * The number of leading ones, from `0` to `32` for IPv4 and from `0` to `128` for IPv6.
     *
     * @since 1.0.0
     */
    public abstract val prefixLength: Int

    /**
     * This netmask as an address of its [version], such as `255.255.255.0` for the IPv4 prefix length `24`.
     *
     * @since 1.0.0
     */
    public abstract val address: IpAddress

    /**
     * The inverse of this netmask as an address, with the host bits set and the prefix bits cleared, such as
     * `0.0.0.255` for the IPv4 prefix length `24`, as access control lists write it.
     *
     * @since 1.0.0
     */
    public abstract val wildcard: IpAddress

    /**
     * The number of host bits, the zeros after the prefix: the bit length of the [version] less the [prefixLength],
     * from `0` to `32` for IPv4 and from `0` to `128` for IPv6.
     *
     * @since 1.0.0
     */
    public val hostBits: Int get() = version.bitLength - prefixLength

    /**
     * Tells whether [other] is a netmask of the same version with the same prefix length.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal netmask; an IPv4 and an IPv6 netmask are never equal.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean =
        other is Netmask && version == other.version && prefixLength == other.prefixLength

    /**
     * Computes a hash code from the version and the prefix length.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = HASH_FACTOR * version.hashCode() + prefixLength

    /**
     * Shows this netmask as its [address], such as `255.255.255.0` or `ffff:ffff:ffff:ffff::`.
     *
     * @return the netmask in the canonical form of its address, which [Ipv4Netmask.parse] or [Ipv6Netmask.parse] reads.
     * @since 1.0.0
     */
    final override fun toString(): String = address.toString()
}
