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
import com.davils.konnect.internetprotocol.address.bits.byteCount
import com.davils.konnect.internetprotocol.address.bits.groupsOf
import com.davils.konnect.internetprotocol.address.bits.shiftedWithin
import com.davils.konnect.internetprotocol.address.grammar.AddressReading
import com.davils.konnect.internetprotocol.address.grammar.readIpv6
import com.davils.konnect.internetprotocol.address.text.canonicalIpv6
import com.davils.konnect.internetprotocol.address.text.fullIpv6
import com.davils.konnect.internetprotocol.address.text.isMapped

private const val GROUPS: Int = 8
private const val GROUP_BITS: Int = 16
private const val GROUP_MAX: Int = 0xFFFF
private const val IPV4_MASK: ULong = 0xFFFF_FFFFuL

/**
 * An IPv6 address of RFC 4291: a number of 128 bits, written as eight hexadecimal groups such as `2001:db8::1`.
 *
 * An address is created from its groups, from its bytes or from text. [toString] shows it in the canonical form of
 * RFC 5952, which [parse] reads back, and [toFullString] shows every group with all four digits. Instances are
 * immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Ipv6Address private constructor(override val bits: Bits128) : IpAddress() {
    /**
     * The version of this address, always [IpVersion.V6].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = IpVersion.V6

    /**
     * The eight groups of 16 bits of this address, the most significant first, each from `0` to `65535`; a new list
     * on every access.
     *
     * @since 1.0.0
     */
    public val groups: List<Int> get() = bits.groupsOf(version, GROUP_BITS)

    /**
     * Returns the address directly after this one.
     *
     * @return the next address, or `null` if this is `ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff`.
     * @since 1.0.0
     */
    override fun next(): Ipv6Address? = shiftedBy(1)

    /**
     * Returns the address directly before this one.
     *
     * @return the previous address, or `null` if this is `::`.
     * @since 1.0.0
     */
    override fun previous(): Ipv6Address? = shiftedBy(-1)

    /**
     * Returns the address [offset] positions after this one, or before it for a negative [offset].
     *
     * The result never wraps around: an offset that would leave the IPv6 address space gives `null`.
     *
     * @param offset the distance to move, negative to move down; every value of [Long] is accepted.
     * @return the shifted address, or `null` if it would be below `::` or above
     * `ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff`.
     * @since 1.0.0
     */
    override fun shiftedBy(offset: Long): Ipv6Address? {
        val shifted = shiftedWithin(version, bits, offset)
        return shifted?.let(::Ipv6Address)
    }

    /**
     * Returns the IPv4 address that this IPv4-mapped address of RFC 4291 carries, such as `192.0.2.1` for
     * `::ffff:192.0.2.1`.
     *
     * Only an address in `::ffff:0:0/96` carries an IPv4 address by this function; the deprecated IPv4-compatible
     * addresses and the addresses of the NAT64 prefixes give `null`.
     *
     * @return the IPv4 address in the last 32 bits, or `null` if this address is not IPv4-mapped.
     * @since 1.0.0
     */
    public fun toIpv4OrNull(): Ipv4Address? {
        if (!isMapped(bits)) return null
        return Ipv4Address.fromBits(Bits128.ofLow(bits.low and IPV4_MASK))
    }

    /**
     * Shows this address with all eight groups and every leading zero, in lower case and without `::`, such as
     * `2001:0db8:0000:0000:0000:0000:0000:0001`.
     *
     * @return the address in its full form, which [parse] reads as well.
     * @since 1.0.0
     */
    public fun toFullString(): String = fullIpv6(bits)

    /**
     * Shows this address in the canonical form of RFC 5952, such as `2001:db8::1`.
     *
     * The groups are written in lower case without leading zeros, and the longest run of at least two zero groups,
     * the first one of equal runs, is replaced by `::`. An IPv4-mapped address ends in its IPv4 address in
     * dotted-decimal form, such as `::ffff:192.0.2.1`.
     *
     * @return the address in the form [parse] reads.
     * @since 1.0.0
     */
    override fun toString(): String = canonicalIpv6(bits)

    /**
     * The well-known IPv6 addresses and the creation of addresses from groups, bytes and text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The unspecified address, `::`, the lowest IPv6 address.
         *
         * @since 1.0.0
         */
        public val UNSPECIFIED: Ipv6Address = Ipv6Address(Bits128.ZERO)

        /**
         * The loopback address, `::1`.
         *
         * @since 1.0.0
         */
        public val LOOPBACK: Ipv6Address = Ipv6Address(Bits128.ONE)

        /**
         * Creates the address from its eight groups of 16 bits, the most significant first, such as
         * `Ipv6Address(listOf(0x2001, 0xdb8, 0, 0, 0, 0, 0, 1))`.
         *
         * The address does not keep [groups]; a later change of the list does not change it.
         *
         * @param groups the eight groups, each from `0` to `65535`.
         * @return the address.
         * @throws IllegalArgumentException if [groups] does not hold exactly eight groups or a group is outside
         * `0..65535`.
         * @since 1.0.0
         */
        public operator fun invoke(groups: List<Int>): Ipv6Address {
            require(groups.size == GROUPS) { "An IPv6 address has $GROUPS groups, but got ${groups.size}" }
            require(groups.all { group -> group in 0..GROUP_MAX }) { "An IPv6 group must be in 0..$GROUP_MAX" }
            val bytes = groups.flatMap { group -> listOf((group ushr Byte.SIZE_BITS).toByte(), group.toByte()) }
            return Ipv6Address(Bits128.fromByteArray(bytes.toByteArray()))
        }

        /**
         * Reads [text] as an IPv6 address in one of the text forms of RFC 4291.
         *
         * The text is eight groups of one to four hexadecimal digits in either case joined by colons, such as
         * `2001:db8:0:0:0:0:0:1`. One `::` may stand for a run of one or more zero groups, such as `2001:db8::1`;
         * a second `::` is refused. The last two groups may be written as an embedded IPv4 address, such as
         * `::ffff:192.0.2.1`, which is read as [Ipv4Address.parse] reads it, without leading zeros. A zone index such
         * as `%eth0`, a prefix length, brackets and whitespace are refused. A text longer than 45 characters, the
         * longest form of an IPv6 address and well within
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH], is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address.
         * @throws IllegalArgumentException if [text] is not an IPv6 address. The message names the problem in fixed
         * words, such as `Not an IPv6 address: it uses '::' more than once`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv6Address = when (val reading = readIpv6(text)) {
            is AddressReading.Read -> Ipv6Address(reading.bits)
            is AddressReading.Refused -> throw IllegalArgumentException("Not an IPv6 address: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv6 address, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv6Address? = when (val reading = readIpv6(text)) {
            is AddressReading.Read -> Ipv6Address(reading.bits)
            is AddressReading.Refused -> null
        }

        /**
         * Creates the address from its bytes in network byte order, the most significant byte first.
         *
         * The address does not keep [bytes]; a later change of the array does not change it.
         *
         * @param bytes the sixteen bytes of the address.
         * @return the address.
         * @throws IllegalArgumentException if [bytes] does not hold exactly sixteen bytes.
         * @since 1.0.0
         */
        public fun fromByteArray(bytes: ByteArray): Ipv6Address {
            val expected = IpVersion.V6.byteCount
            require(bytes.size == expected) { "An IPv6 address has $expected bytes, but got ${bytes.size}" }
            return Ipv6Address(Bits128.fromByteArray(bytes))
        }

        internal fun fromBits(bits: Bits128): Ipv6Address = Ipv6Address(bits)
    }
}
