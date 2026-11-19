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

package com.davils.konnect.internetprotocol.subnet

import com.davils.konnect.core.Limits
import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.mask.Ipv6Netmask
import com.davils.konnect.internetprotocol.range.Ipv6Range
import com.davils.konnect.internetprotocol.subnet.arithmetic.nextBlock
import com.davils.konnect.internetprotocol.subnet.arithmetic.previousBlock
import com.davils.konnect.internetprotocol.subnet.arithmetic.splitPrefixLength
import com.davils.konnect.internetprotocol.subnet.arithmetic.subnetBlocks
import com.davils.konnect.internetprotocol.subnet.grammar.CidrReading
import com.davils.konnect.internetprotocol.subnet.grammar.readNetwork
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "network"
private val VERSION: IpVersion = IpVersion.V6

/**
 * An IPv6 network in CIDR notation, such as `2001:db8::/32`: the block of addresses that share their first
 * [prefixLength] bits.
 *
 * Its [address] has no host bits set; an address with host bits set is refused, never masked. A network is created with
 * [invoke], read with [parse] or built with [ipv6Network][com.davils.konnect.internetprotocol.subnet.ipv6Network].
 *
 * IPv6 has no broadcast address, so every address of a network counts as a host: [firstHost] is [firstAddress],
 * [lastHost] is [lastAddress] and [hostCount] is [addressCount]. This includes the Subnet-Router anycast address of RFC
 * 4291, the first address of the network. Instances are immutable.
 *
 * @since 1.0.0
 */
public class Ipv6Network private constructor(
    override val bits: Bits128,
    override val prefixLength: Int,
) : IpNetwork() {
    /**
     * The version of this network, always [IpVersion.V6].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = VERSION

    /**
     * The address of the network: its first address, with every host bit cleared, such as `2001:db8::` for
     * `2001:db8::/32`.
     *
     * @since 1.0.0
     */
    override val address: Ipv6Address get() = Ipv6Address.fromBits(bits)

    /**
     * The netmask of [prefixLength], such as `ffff:ffff::` for `/32`.
     *
     * @since 1.0.0
     */
    override val netmask: Ipv6Netmask get() = Ipv6Netmask.ofPrefix(prefixLength)

    /**
     * The wildcard mask, the inverse of [netmask] with every host bit set, such as `::ffff:ffff:ffff:ffff:ffff:ffff`
     * for `/32`.
     *
     * @since 1.0.0
     */
    override val wildcard: Ipv6Address get() = netmask.wildcard

    /**
     * The first address of the network, which equals [address].
     *
     * @since 1.0.0
     */
    override val firstAddress: Ipv6Address get() = address

    /**
     * The last address of the network, with every host bit set, such as `2001:db8:ffff:ffff:ffff:ffff:ffff:ffff` for
     * `2001:db8::/32`.
     *
     * @since 1.0.0
     */
    override val lastAddress: Ipv6Address get() = Ipv6Address.fromBits(lastBits)

    /**
     * The first address a host can use, which equals [firstAddress], as IPv6 reserves none.
     *
     * @since 1.0.0
     */
    override val firstHost: Ipv6Address get() = firstAddress

    /**
     * The last address a host can use, which equals [lastAddress], as IPv6 has no broadcast address.
     *
     * @since 1.0.0
     */
    override val lastHost: Ipv6Address get() = lastAddress

    /**
     * The number of addresses from [firstHost] to [lastHost], which equals [addressCount]; exact up to `2^128`.
     *
     * @since 1.0.0
     */
    override val hostCount: AddressCount get() = addressCount

    /**
     * Lists the subnets of this network with [prefixLength], in ascending order.
     *
     * The subnets cover this network without gaps and without overlaps, such as `2001:db8::/33` and
     * `2001:db8:8000::/33` for `2001:db8::/32` with `33`. With the prefix length of this network, the only subnet is
     * this network. The sequence doubles in length with every bit by which [prefixLength] exceeds the prefix length of
     * this network, up to `2^128` networks, and it is computed lazily, so a caller must bound how many it takes when
     * [prefixLength] may come from an attacker.
     *
     * @param prefixLength the prefix length of the subnets, from the prefix length of this network to `128`.
     * @return the subnets, as a lazy sequence that can be iterated more than once.
     * @throws IllegalArgumentException if [prefixLength] is outside the prefix length of this network to `128`. The
     * message names this network, the allowed range and the prefix length in fixed words.
     * @since 1.0.0
     */
    public fun subnets(prefixLength: Int): Sequence<Ipv6Network> {
        requireSubnetPrefixLength(prefixLength)
        return subnetBlocks(block, VERSION, prefixLength).map { bits -> Ipv6Network(bits, prefixLength) }
    }

    /**
     * Splits this network into at least [count] subnets of equal size.
     *
     * The subnets have the shortest prefix length that yields at least [count] of them, so [count] is rounded up to the
     * next power of two: `2001:db8::/32` split into `3` yields four `/34` networks. A [count] of `1` yields this
     * network. The list is built at once, so
     * [count] is bounded by [Limits.MAX_SPLIT_COUNT][com.davils.konnect.core.Limits.MAX_SPLIT_COUNT].
     *
     * @param count the least number of subnets, at least `1`.
     * @return the subnets in ascending order, or `null` if this network is too small for [count] subnets, when their
     * prefix length would exceed `128`.
     * @throws IllegalArgumentException if [count] is below `1` or above
     * [Limits.MAX_SPLIT_COUNT][com.davils.konnect.core.Limits.MAX_SPLIT_COUNT].
     * @since 1.0.0
     */
    public fun split(count: Int): List<Ipv6Network>? {
        require(count in 1..Limits.MAX_SPLIT_COUNT) {
            "A network splits into 1 to ${Limits.MAX_SPLIT_COUNT} subnets at once, but got $count"
        }
        val prefixLength = splitPrefixLength(this.prefixLength, count, VERSION) ?: return null
        val subnets = subnets(prefixLength)
        return buildList { addAll(subnets) }
    }

    /**
     * Returns the network with [prefixLength] that contains this network, such as `2001:d00::/24` for `2001:db8::/32`
     * with `24`.
     *
     * With the prefix length of this network, it returns a network equal to this one, and with `0` the network of the
     * whole address space.
     *
     * @param prefixLength the prefix length of the supernet, from `0` to the prefix length of this network.
     * @return the supernet.
     * @throws IllegalArgumentException if [prefixLength] is outside `0` to the prefix length of this network. The
     * message names this network, the allowed range and the prefix length in fixed words.
     * @since 1.0.0
     */
    public fun supernet(prefixLength: Int): Ipv6Network {
        requireSupernetPrefixLength(prefixLength)
        return Ipv6Network(bits intersect prefixMask(VERSION, prefixLength), prefixLength)
    }

    /**
     * Returns the network with the same prefix length that directly follows this network, such as `2001:db9::/32` after
     * `2001:db8::/32`.
     *
     * @return the following network, or `null` if this network ends at the last address of the address space,
     * `ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff`.
     * @since 1.0.0
     */
    public fun nextSibling(): Ipv6Network? {
        val next = nextBlock(bits, VERSION, prefixLength)
        return next?.let { bits -> Ipv6Network(bits, prefixLength) }
    }

    /**
     * Returns the network with the same prefix length that directly precedes this network, such as `2001:db7::/32`
     * before `2001:db8::/32`.
     *
     * @return the preceding network, or `null` if this network starts at the first address of the address space, `::`.
     * @since 1.0.0
     */
    public fun previousSibling(): Ipv6Network? {
        val previous = previousBlock(bits, VERSION, prefixLength)
        return previous?.let { bits -> Ipv6Network(bits, prefixLength) }
    }

    /**
     * Returns the range of all addresses of this network, from [firstAddress] to [lastAddress].
     *
     * @return the range.
     * @since 1.0.0
     */
    public fun toRange(): Ipv6Range = Ipv6Range.assembled(first = firstAddress, last = lastAddress)

    /**
     * Lists the addresses from [firstHost] to [lastHost] in ascending order.
     *
     * The sequence is computed lazily and holds [hostCount] addresses, `2^64` for a single `/64`, so a caller must
     * bound how many it takes.
     *
     * @return the host addresses, as a lazy sequence that can be iterated more than once.
     * @since 1.0.0
     */
    public fun hosts(): Sequence<Ipv6Address> =
        generateSequence(firstHost) { host -> host.next()?.takeIf { next -> next <= lastHost } }

    /**
     * The creation of IPv6 networks and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates the network of [address] with [prefixLength].
         *
         * An [address] with host bits set is refused, never masked: `2001:db8::10/32` is not a network, but
         * `2001:db8::/32` is.
         *
         * @param address the address of the network; it must have no host bits set.
         * @param prefixLength the length of the prefix, from `0` to `128`.
         * @return the network.
         * @throws com.davils.kore.core.validation.report.ValidationException if [prefixLength] is outside `0..128`,
         * with one error at `prefixLength` with the code
         * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX], or if
         * [address] has host bits set, with one error at `address` with the code
         * [IpViolationCodes.HOST_BITS_SET][com.davils.konnect.internetprotocol.IpViolationCodes.HOST_BITS_SET] whose
         * message names the network address with the host bits cleared.
         * @since 1.0.0
         */
        public operator fun invoke(address: Ipv6Address, prefixLength: Int): Ipv6Network =
            assembled(address, prefixLength).validateOrThrow(SUBJECT)

        /**
         * Reads [text] as an IPv6 network.
         *
         * The text is an IPv6 address as [Ipv6Address.parse] reads it, in any form of RFC 4291, also compressed with
         * `::` and with an embedded IPv4 address, but without a zone index, optionally followed by a slash and a prefix
         * length from `0` to `128` in decimal without leading zeros, such as `2001:db8::/32`. A netmask is not accepted
         * after the slash. Without a slash, the prefix length is `128`. No whitespace is allowed anywhere, and a text
         * longer than [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is
         * read.
         *
         * An address with host bits set is refused, never masked: `2001:db8::10/32` is not a network.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network.
         * @throws IllegalArgumentException if [text] is not an IPv6 network. The message names the problem in fixed
         * words and never quotes [text]; for host bits set, it names the network meant instead, such as
         * `Not an IPv6 network: it has host bits set beyond its prefix length, the network is 2001:db8::/32`.
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv6Network = when (val reading = readNetwork(text, VERSION)) {
            is CidrReading.Read -> Ipv6Network(reading.bits, reading.prefixLength)
            is CidrReading.Refused -> throw IllegalArgumentException("Not an IPv6 network: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv6 network, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv6Network? = when (val reading = readNetwork(text, VERSION)) {
            is CidrReading.Read -> Ipv6Network(reading.bits, reading.prefixLength)
            is CidrReading.Refused -> null
        }

        internal fun fromBits(bits: Bits128, prefixLength: Int): Ipv6Network = Ipv6Network(bits, prefixLength)

        internal fun assembled(address: Ipv6Address, prefixLength: Int): Ipv6Network =
            Ipv6Network(address.bits, prefixLength)
    }
}
