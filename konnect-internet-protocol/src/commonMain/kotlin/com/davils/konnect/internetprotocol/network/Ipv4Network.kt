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
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.mask.Ipv4Netmask
import com.davils.konnect.internetprotocol.range.Ipv4Range
import com.davils.konnect.internetprotocol.subnet.arithmetic.nextBlock
import com.davils.konnect.internetprotocol.subnet.arithmetic.previousBlock
import com.davils.konnect.internetprotocol.subnet.arithmetic.splitPrefixLength
import com.davils.konnect.internetprotocol.subnet.arithmetic.subnetBlocks
import com.davils.konnect.internetprotocol.subnet.grammar.CidrReading
import com.davils.konnect.internetprotocol.subnet.grammar.readNetwork
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "network"
private const val BROADCAST_HOST_BITS: Int = 2
private val VERSION: IpVersion = IpVersion.V4

/**
 * An IPv4 network in CIDR notation, such as `192.0.2.0/24`: the block of addresses that share their first
 * [prefixLength] bits.
 *
 * Its [address] has no host bits set; an address with host bits set is refused, never masked. A network is created with
 * [invoke], read with [parse] or built with [ipv4Network][com.davils.konnect.internetprotocol.subnet.ipv4Network].
 *
 * A network of `/30` or shorter keeps its first address for the network and its last address for the [broadcast], so
 * its hosts are the addresses between them. A `/31` has no broadcast address and both of its addresses are hosts, as
 * RFC 3021 allows on point-to-point links, and a `/32` is a single host. Instances are immutable.
 *
 * @since 1.0.0
 */
public class Ipv4Network private constructor(
    override val bits: Bits128,
    override val prefixLength: Int,
) : IpNetwork() {
    private val hasBroadcast: Boolean get() = hostBits >= BROADCAST_HOST_BITS

    /**
     * The version of this network, always [IpVersion.V4].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = VERSION

    /**
     * The address of the network: its first address, with every host bit cleared, such as `192.0.2.0` for
     * `192.0.2.0/24`.
     *
     * @since 1.0.0
     */
    override val address: Ipv4Address get() = Ipv4Address.fromBits(bits)

    /**
     * The netmask of [prefixLength], such as `255.255.255.0` for `/24`.
     *
     * @since 1.0.0
     */
    override val netmask: Ipv4Netmask get() = Ipv4Netmask.ofPrefix(prefixLength)

    /**
     * The wildcard mask, the inverse of [netmask] with every host bit set, such as `0.0.0.255` for `/24`.
     *
     * @since 1.0.0
     */
    override val wildcard: Ipv4Address get() = netmask.wildcard

    /**
     * The first address of the network, which equals [address].
     *
     * @since 1.0.0
     */
    override val firstAddress: Ipv4Address get() = address

    /**
     * The last address of the network, with every host bit set, such as `192.0.2.255` for `192.0.2.0/24`.
     *
     * @since 1.0.0
     */
    override val lastAddress: Ipv4Address get() = Ipv4Address.fromBits(lastBits)

    /**
     * The first address a host can use: the address after [address] in a network of `/30` or shorter, such as
     * `192.0.2.1` for `192.0.2.0/24`, and [firstAddress] in a `/31` or `/32`.
     *
     * @since 1.0.0
     */
    override val firstHost: Ipv4Address get() = firstAddress.next().takeIf { hasBroadcast } ?: firstAddress

    /**
     * The last address a host can use: the address before [broadcast] in a network of `/30` or shorter, such as
     * `192.0.2.254` for `192.0.2.0/24`, and [lastAddress] in a `/31` or `/32`.
     *
     * @since 1.0.0
     */
    override val lastHost: Ipv4Address get() = lastAddress.previous().takeIf { hasBroadcast } ?: lastAddress

    /**
     * The number of addresses from [firstHost] to [lastHost]: [addressCount] less two in a network of `/30` or shorter,
     * such as `254` for a `/24`, `2` for a `/31` and `1` for a `/32`.
     *
     * @since 1.0.0
     */
    override val hostCount: AddressCount get() = addressCount.lessTwo().takeIf { hasBroadcast } ?: addressCount

    /**
     * The broadcast address, the [lastAddress] of a network of `/30` or shorter, such as `192.0.2.255` for
     * `192.0.2.0/24`; `null` for a `/31` or `/32`, which has none.
     *
     * @since 1.0.0
     */
    public val broadcast: Ipv4Address? get() = lastAddress.takeIf { hasBroadcast }

    /**
     * Lists the subnets of this network with [prefixLength], in ascending order.
     *
     * The subnets cover this network without gaps and without overlaps, such as `192.0.2.0/25` and `192.0.2.128/25` for
     * `192.0.2.0/24` with `25`. With the prefix length of this network, the only subnet is this network. The sequence
     * doubles in length with every bit by which [prefixLength] exceeds the prefix length of this network, up to `2^32`
     * networks, and it is computed lazily, so a caller must bound how many it takes when [prefixLength] may come from
     * an attacker.
     *
     * @param prefixLength the prefix length of the subnets, from the prefix length of this network to `32`.
     * @return the subnets, as a lazy sequence that can be iterated more than once.
     * @throws IllegalArgumentException if [prefixLength] is outside the prefix length of this network to `32`. The
     * message names this network, the allowed range and the prefix length in fixed words.
     * @since 1.0.0
     */
    public fun subnets(prefixLength: Int): Sequence<Ipv4Network> {
        requireSubnetPrefixLength(prefixLength)
        return subnetBlocks(block, VERSION, prefixLength).map { bits -> Ipv4Network(bits, prefixLength) }
    }

    /**
     * Splits this network into at least [count] subnets of equal size.
     *
     * The subnets have the shortest prefix length that yields at least [count] of them, so [count] is rounded up to the
     * next power of two: `192.0.2.0/24` split into `3` yields four `/26` networks. A [count] of `1` yields this
     * network. The list is built at once, so
     * [count] is bounded by [Limits.MAX_SPLIT_COUNT][com.davils.konnect.core.Limits.MAX_SPLIT_COUNT].
     *
     * @param count the least number of subnets, at least `1`.
     * @return the subnets in ascending order, or `null` if this network is too small for [count] subnets, when their
     * prefix length would exceed `32`.
     * @throws IllegalArgumentException if [count] is below `1` or above
     * [Limits.MAX_SPLIT_COUNT][com.davils.konnect.core.Limits.MAX_SPLIT_COUNT].
     * @since 1.0.0
     */
    public fun split(count: Int): List<Ipv4Network>? {
        require(count in 1..Limits.MAX_SPLIT_COUNT) {
            "A network splits into 1 to ${Limits.MAX_SPLIT_COUNT} subnets at once, but got $count"
        }
        val prefixLength = splitPrefixLength(this.prefixLength, count, VERSION) ?: return null
        val subnets = subnets(prefixLength)
        return buildList { addAll(subnets) }
    }

    /**
     * Returns the network with [prefixLength] that contains this network, such as `192.0.0.0/16` for `192.0.2.0/24`
     * with `16`.
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
    public fun supernet(prefixLength: Int): Ipv4Network {
        requireSupernetPrefixLength(prefixLength)
        return Ipv4Network(bits intersect prefixMask(VERSION, prefixLength), prefixLength)
    }

    /**
     * Returns the network with the same prefix length that directly follows this network, such as `192.0.3.0/24` after
     * `192.0.2.0/24`.
     *
     * @return the following network, or `null` if this network ends at the last address of the address space,
     * `255.255.255.255`.
     * @since 1.0.0
     */
    public fun nextSibling(): Ipv4Network? {
        val next = nextBlock(bits, VERSION, prefixLength)
        return next?.let { bits -> Ipv4Network(bits, prefixLength) }
    }

    /**
     * Returns the network with the same prefix length that directly precedes this network, such as `192.0.1.0/24`
     * before `192.0.2.0/24`.
     *
     * @return the preceding network, or `null` if this network starts at the first address of the address space,
     * `0.0.0.0`.
     * @since 1.0.0
     */
    public fun previousSibling(): Ipv4Network? {
        val previous = previousBlock(bits, VERSION, prefixLength)
        return previous?.let { bits -> Ipv4Network(bits, prefixLength) }
    }

    /**
     * Returns the range of all addresses of this network, from [firstAddress] to [lastAddress].
     *
     * @return the range.
     * @since 1.0.0
     */
    public fun toRange(): Ipv4Range = Ipv4Range.assembled(first = firstAddress, last = lastAddress)

    /**
     * Lists the addresses from [firstHost] to [lastHost] in ascending order.
     *
     * The sequence is computed lazily and holds [hostCount] addresses, over four billion for `0.0.0.0/0`, so a caller
     * must bound how many it takes.
     *
     * @return the host addresses, as a lazy sequence that can be iterated more than once.
     * @since 1.0.0
     */
    public fun hosts(): Sequence<Ipv4Address> =
        generateSequence(firstHost) { host -> host.next()?.takeIf { next -> next <= lastHost } }

    /**
     * The creation of IPv4 networks and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates the network of [address] with [prefixLength].
         *
         * An [address] with host bits set is refused, never masked: `192.0.2.10/24` is not a network, but
         * `192.0.2.0/24` is.
         *
         * @param address the address of the network; it must have no host bits set.
         * @param prefixLength the length of the prefix, from `0` to `32`.
         * @return the network.
         * @throws com.davils.kore.core.validation.report.ValidationException if [prefixLength] is outside `0..32`, with
         * one error at `prefixLength` with the code
         * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX], or if
         * [address] has host bits set, with one error at `address` with the code
         * [IpViolationCodes.HOST_BITS_SET][com.davils.konnect.internetprotocol.IpViolationCodes.HOST_BITS_SET] whose
         * message names the network address with the host bits cleared.
         * @since 1.0.0
         */
        public operator fun invoke(address: Ipv4Address, prefixLength: Int): Ipv4Network =
            assembled(address, prefixLength).validateOrThrow(SUBJECT)

        /**
         * Reads [text] as an IPv4 network.
         *
         * The text is an IPv4 address as [Ipv4Address.parse] reads it, four decimal numbers from `0` to `255` without
         * leading zeros, optionally followed by a slash and either a prefix length from `0` to `32` in decimal without
         * leading zeros, such as `192.0.2.0/24`, or a netmask in dotted-quad form whose ones are contiguous, such as
         * `192.0.2.0/255.255.255.0`. Without a slash, the prefix length is `32`. No whitespace is allowed anywhere, and
         * a text longer than [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before
         * it is read.
         *
         * An address with host bits set is refused, never masked: `192.0.2.10/24` is not a network.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network.
         * @throws IllegalArgumentException if [text] is not an IPv4 network. The message names the problem in fixed
         * words and never quotes [text]; for host bits set, it names the network meant instead, such as
         * `Not an IPv4 network: it has host bits set beyond its prefix length, the network is 192.0.2.0/24`.
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv4Network = when (val reading = readNetwork(text, VERSION)) {
            is CidrReading.Read -> Ipv4Network(reading.bits, reading.prefixLength)
            is CidrReading.Refused -> throw IllegalArgumentException("Not an IPv4 network: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv4 network, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv4Network? = when (val reading = readNetwork(text, VERSION)) {
            is CidrReading.Read -> Ipv4Network(reading.bits, reading.prefixLength)
            is CidrReading.Refused -> null
        }

        internal fun fromBits(bits: Bits128, prefixLength: Int): Ipv4Network = Ipv4Network(bits, prefixLength)

        internal fun assembled(address: Ipv4Address, prefixLength: Int): Ipv4Network =
            Ipv4Network(address.bits, prefixLength)
    }
}
