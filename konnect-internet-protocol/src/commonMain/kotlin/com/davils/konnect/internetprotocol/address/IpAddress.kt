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

package com.davils.konnect.internetprotocol.address

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.toByteArray
import com.davils.konnect.internetprotocol.address.grammar.isIpv6Text
import com.davils.konnect.internetprotocol.address.registry.SpecialPurposeRegistry
import com.davils.konnect.internetprotocol.address.text.binaryText
import com.davils.konnect.internetprotocol.address.text.hexText
import com.davils.konnect.internetprotocol.address.text.reverseDnsName

private const val HASH_FACTOR: Int = 31

/**
 * An IPv4 or an IPv6 address: an [Ipv4Address] or an [Ipv6Address].
 *
 * Addresses are ordered first by their [version], every IPv4 address before every IPv6 address, and then by their
 * numeric value. Two addresses are equal only if they have the same version and the same value, so `192.0.2.1` and
 * its IPv4-mapped form `::ffff:192.0.2.1` are different addresses. [toString] shows the canonical text of the
 * address, which [parse] reads back. Instances are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed class IpAddress : Comparable<IpAddress> {
    internal abstract val bits: Bits128

    /**
     * The version of the Internet Protocol this address belongs to.
     *
     * @since 1.0.0
     */
    public abstract val version: IpVersion

    /**
     * The name of this address in the reverse DNS of RFC 1035 and RFC 3596, without a trailing dot: the octets in
     * reverse order under `in-addr.arpa` for IPv4, such as `1.2.0.192.in-addr.arpa` for `192.0.2.1`, and the 32
     * hexadecimal nibbles in reverse order under `ip6.arpa` for IPv6.
     *
     * @since 1.0.0
     */
    public val reverseDnsName: String get() = reverseDnsName(bits, version)

    /**
     * The purpose of this address in the IANA special-purpose address registries; [AddressCategory.Public] if no
     * block of the registries holds it.
     *
     * @since 1.0.0
     */
    public val category: AddressCategory get() = SpecialPurposeRegistry.purposeOf(this).category

    /**
     * Whether this address is globally reachable, as the IANA special-purpose address registries define it.
     *
     * Every address of the category [AddressCategory.Public] is globally reachable, and so are a few addresses of
     * the categories [AddressCategory.ProtocolAssignment] and [AddressCategory.Translation], such as the well-known
     * NAT64 prefix `64:ff9b::/96`. Every other special-purpose address is not.
     *
     * @since 1.0.0
     */
    public val isPublic: Boolean get() = SpecialPurposeRegistry.purposeOf(this).isGloballyReachable

    /**
     * Whether this address is of the category [AddressCategory.Private] of RFC 1918 or [AddressCategory.UniqueLocal]
     * of RFC 4193. The shared address space of RFC 6598 is not private by this definition.
     *
     * @since 1.0.0
     */
    public val isPrivate: Boolean get() = category == AddressCategory.Private ||
        category == AddressCategory.UniqueLocal

    /**
     * Whether this address is of the category [AddressCategory.Loopback]: in `127.0.0.0/8` or `::1`.
     *
     * @since 1.0.0
     */
    public val isLoopback: Boolean get() = category == AddressCategory.Loopback

    /**
     * Whether this address is of the category [AddressCategory.LinkLocal]: in `169.254.0.0/16` or `fe80::/10`.
     *
     * @since 1.0.0
     */
    public val isLinkLocal: Boolean get() = category == AddressCategory.LinkLocal

    /**
     * Whether this address is of the category [AddressCategory.Multicast]: in `224.0.0.0/4` or `ff00::/8`.
     *
     * The multicast documentation block `233.252.0.0/24` is of the category [AddressCategory.Documentation], so its
     * addresses are not multicast by this property.
     *
     * @since 1.0.0
     */
    public val isMulticast: Boolean get() = category == AddressCategory.Multicast

    /**
     * Whether this address is the unspecified address, `0.0.0.0` or `::`.
     *
     * @since 1.0.0
     */
    public val isUnspecified: Boolean get() = category == AddressCategory.Unspecified

    /**
     * Returns the address directly after this one.
     *
     * @return the next address of the same version, or `null` if this is the highest address of its version.
     * @since 1.0.0
     */
    public abstract fun next(): IpAddress?

    /**
     * Returns the address directly before this one.
     *
     * @return the previous address of the same version, or `null` if this is the lowest address of its version.
     * @since 1.0.0
     */
    public abstract fun previous(): IpAddress?

    /**
     * Returns the address [offset] positions after this one, or before it for a negative [offset].
     *
     * The result never wraps around: an offset that would leave the address space of the version gives `null`.
     *
     * @param offset the distance to move, negative to move down; every value of [Long] is accepted.
     * @return the shifted address of the same version, or `null` if it would be below the lowest or above the
     * highest address of the version.
     * @since 1.0.0
     */
    public abstract fun shiftedBy(offset: Long): IpAddress?

    /**
     * Returns the bytes of this address in network byte order, the most significant byte first.
     *
     * @return a new array of 4 bytes for IPv4 or 16 bytes for IPv6, which the caller may change freely.
     * @since 1.0.0
     */
    public fun toByteArray(): ByteArray = bits.toByteArray(version)

    /**
     * Shows this address in binary: for IPv4 the four octets as eight binary digits each, joined by dots, such as
     * `11000000.00000000.00000010.00000001`; for IPv6 the eight groups as sixteen binary digits each, joined by
     * colons.
     *
     * @return the address in binary, with every leading zero.
     * @since 1.0.0
     */
    public fun toBinaryString(): String = binaryText(bits, version)

    /**
     * Shows this address as one hexadecimal number in lower case without separators, with every leading zero, such
     * as `c0000201` for `192.0.2.1`: 8 digits for IPv4 and 32 digits for IPv6.
     *
     * @return the address in hexadecimal.
     * @since 1.0.0
     */
    public fun toHexString(): String = hexText(bits, version)

    /**
     * Compares this address with [other], first by their versions and then by their numeric values.
     *
     * @param other the address to compare with.
     * @return a negative number, zero or a positive number as this address is below, equal to or above [other]; an
     * IPv4 address is below every IPv6 address.
     * @since 1.0.0
     */
    final override fun compareTo(other: IpAddress): Int {
        val byVersion = version.compareTo(other.version)
        if (byVersion != 0) return byVersion
        return bits.compareTo(other.bits)
    }

    /**
     * Tells whether [other] is an address of the same version and the same value.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal address; an IPv4 address never equals an IPv6 address.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean =
        other is IpAddress && version == other.version && bits == other.bits

    /**
     * Computes a hash code from the version and the value.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = HASH_FACTOR * version.hashCode() + bits.hashCode()

    /**
     * The reading of addresses of either version from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as an IPv4 or an IPv6 address.
         *
         * A text that holds a colon is read as [Ipv6Address.parse] reads it, every other text as [Ipv4Address.parse]
         * reads it, with the same forms and limits. Neither accepts surrounding whitespace, a zone index or a
         * prefix length.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address.
         * @throws IllegalArgumentException if [text] is not an address. The message names the problem in fixed
         * words, such as `Not an IPv4 address: it has a number with a leading zero`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): IpAddress {
            if (isIpv6Text(text)) return Ipv6Address.parse(text)
            return Ipv4Address.parse(text)
        }

        /**
         * Reads [text] as an IPv4 or an IPv6 address, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpAddress? {
            if (isIpv6Text(text)) return Ipv6Address.parseOrNull(text)
            return Ipv4Address.parseOrNull(text)
        }
    }
}
