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
import com.davils.konnect.internetprotocol.address.grammar.readIpv4
import com.davils.konnect.internetprotocol.address.text.dottedQuad

private const val OCTETS: Int = 4
private const val OCTET_MAX: Int = 255
private const val MAPPED_MARK: ULong = 0xFFFF_0000_0000uL
private const val LOOPBACK_BITS: ULong = 0x7F00_0001uL
private const val BROADCAST_BITS: ULong = 0xFFFF_FFFFuL

/**
 * An IPv4 address of RFC 791: a number of 32 bits, written as four decimal octets such as `192.0.2.1`.
 *
 * An address is created from its octets, from its bytes or from text. [toString] shows it in dotted-decimal form
 * without leading zeros, which [parse] reads back. Instances are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Ipv4Address private constructor(override val bits: Bits128) : IpAddress() {
    /**
     * The version of this address, always [IpVersion.V4].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = IpVersion.V4

    /**
     * The four octets of this address, the most significant first, each from `0` to `255`; a new list on every
     * access.
     *
     * @since 1.0.0
     */
    public val octets: List<Int> get() = bits.groupsOf(version, Byte.SIZE_BITS)

    /**
     * The historic class of this address by the leading bits of its first octet.
     *
     * @since 1.0.0
     */
    public val addressClass: Ipv4Class get() = Ipv4Class.forFirstOctet(octets.first())

    /**
     * Returns the address directly after this one.
     *
     * @return the next address, or `null` if this is `255.255.255.255`.
     * @since 1.0.0
     */
    override fun next(): Ipv4Address? = shiftedBy(1)

    /**
     * Returns the address directly before this one.
     *
     * @return the previous address, or `null` if this is `0.0.0.0`.
     * @since 1.0.0
     */
    override fun previous(): Ipv4Address? = shiftedBy(-1)

    /**
     * Returns the address [offset] positions after this one, or before it for a negative [offset].
     *
     * The result never wraps around: an offset that would leave `0.0.0.0..255.255.255.255` gives `null`.
     *
     * @param offset the distance to move, negative to move down; every value of [Long] is accepted.
     * @return the shifted address, or `null` if it would be below `0.0.0.0` or above `255.255.255.255`.
     * @since 1.0.0
     */
    override fun shiftedBy(offset: Long): Ipv4Address? {
        val shifted = shiftedWithin(version, bits, offset)
        return shifted?.let(::Ipv4Address)
    }

    /**
     * Returns the IPv4-mapped IPv6 address of RFC 4291 that carries this address, such as `::ffff:192.0.2.1`.
     *
     * [Ipv6Address.toIpv4OrNull] turns the result back into this address.
     *
     * @return the IPv6 address in `::ffff:0:0/96` whose last 32 bits are this address.
     * @since 1.0.0
     */
    public fun toMappedIpv6(): Ipv6Address = Ipv6Address.fromBits(Bits128.ofLow(MAPPED_MARK or bits.low))

    /**
     * Shows this address in dotted-decimal form, four octets in decimal without leading zeros, such as `192.0.2.1`.
     *
     * @return the address in the form [parse] reads.
     * @since 1.0.0
     */
    override fun toString(): String = dottedQuad(bits)

    /**
     * The well-known IPv4 addresses and the creation of addresses from octets, bytes and text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The unspecified address, `0.0.0.0`, the lowest IPv4 address.
         *
         * @since 1.0.0
         */
        public val UNSPECIFIED: Ipv4Address = Ipv4Address(Bits128.ZERO)

        /**
         * The usual loopback address, `127.0.0.1`.
         *
         * @since 1.0.0
         */
        public val LOOPBACK: Ipv4Address = Ipv4Address(Bits128.ofLow(LOOPBACK_BITS))

        /**
         * The limited broadcast address, `255.255.255.255`, the highest IPv4 address.
         *
         * @since 1.0.0
         */
        public val BROADCAST: Ipv4Address = Ipv4Address(Bits128.ofLow(BROADCAST_BITS))

        /**
         * Creates the address from its four octets, the most significant first, such as `Ipv4Address(192, 0, 2, 1)`.
         *
         * @param first the first octet, from `0` to `255`.
         * @param second the second octet, from `0` to `255`.
         * @param third the third octet, from `0` to `255`.
         * @param fourth the fourth octet, from `0` to `255`.
         * @return the address.
         * @throws IllegalArgumentException if an octet is outside `0..255`; the message shows the four octets.
         * @since 1.0.0
         */
        public operator fun invoke(first: Int, second: Int, third: Int, fourth: Int): Ipv4Address {
            val octets = listOf(first, second, third, fourth)
            require(octets.all { octet -> octet in 0..OCTET_MAX }) {
                "An IPv4 octet must be in 0..$OCTET_MAX, but was $octets"
            }
            val bytes = ByteArray(OCTETS) { index -> octets[index].toByte() }
            return Ipv4Address(Bits128.fromByteArray(bytes))
        }

        /**
         * Reads [text] as an IPv4 address.
         *
         * The text must be four decimal numbers from `0` to `255` joined by dots, such as `192.0.2.1`. A number must
         * not have a leading zero, which some readers take for octal, nor a sign; whitespace, a prefix length and
         * the shortened, hexadecimal, octal and single-number forms of `inet_aton`, such as `127.1` or `0x7f.0.0.1`,
         * are refused. A text longer than [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH]
         * is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address.
         * @throws IllegalArgumentException if [text] is not an IPv4 address. The message names the problem in fixed
         * words, such as `Not an IPv4 address: it has a number with a leading zero`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv4Address = when (val reading = readIpv4(text)) {
            is AddressReading.Read -> Ipv4Address(reading.bits)
            is AddressReading.Refused -> throw IllegalArgumentException("Not an IPv4 address: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv4 address, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the address, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv4Address? = when (val reading = readIpv4(text)) {
            is AddressReading.Read -> Ipv4Address(reading.bits)
            is AddressReading.Refused -> null
        }

        /**
         * Creates the address from its bytes in network byte order, the most significant byte first.
         *
         * The address does not keep [bytes]; a later change of the array does not change it.
         *
         * @param bytes the four bytes of the address.
         * @return the address.
         * @throws IllegalArgumentException if [bytes] does not hold exactly four bytes.
         * @since 1.0.0
         */
        public fun fromByteArray(bytes: ByteArray): Ipv4Address {
            require(bytes.size == IpVersion.V4.byteCount) {
                "An IPv4 address has $OCTETS bytes, but got ${bytes.size}"
            }
            return Ipv4Address(Bits128.fromByteArray(bytes))
        }

        internal fun fromBits(bits: Bits128): Ipv4Address = Ipv4Address(bits)
    }
}
