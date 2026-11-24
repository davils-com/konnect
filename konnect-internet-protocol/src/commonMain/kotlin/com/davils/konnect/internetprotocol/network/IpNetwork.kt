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

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.hasHostBits
import com.davils.konnect.internetprotocol.address.bits.hostMask
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.address.grammar.isIpv6Text
import com.davils.konnect.internetprotocol.address.text.addressText
import com.davils.konnect.internetprotocol.mask.Netmask
import com.davils.konnect.internetprotocol.subnet.arithmetic.CidrBlock
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val HASH_FACTOR: Int = 31

/**
 * A network in CIDR notation: the block of addresses that share their first [prefixLength] bits, such as
 * `192.0.2.0/24` or `2001:db8::/32`.
 *
 * The [address] of a network is the first address of its block and has no host bits set. A network is never created
 * from an address with host bits set: such an address is refused, never masked silently. A host address with a prefix
 * length is an [IpInterface] instead. A network is either an [Ipv4Network] or an [Ipv6Network].
 *
 * Two networks are equal when they have the same address and the same prefix length. They are ordered by their
 * address, IPv4 before IPv6, and then by their prefix length, so a network comes before its subnets that start at
 * the same address. [toString] shows the canonical form, such as `192.0.2.0/24` or `2001:db8::/32`. Instances are
 * immutable.
 *
 * @since 1.0.0
 */
public sealed class IpNetwork : Comparable<IpNetwork>, Validatable {
    internal abstract val bits: Bits128

    /**
     * The version of the Internet Protocol of this network.
     *
     * @since 1.0.0
     */
    public abstract val version: IpVersion

    /**
     * The length of the prefix, from `0` to `32` for IPv4 and from `0` to `128` for IPv6.
     *
     * @since 1.0.0
     */
    public abstract val prefixLength: Int

    internal val hostBits: Int get() = version.bitLength - prefixLength

    internal val lastBits: Bits128 get() = bits union hostMask(version, prefixLength)

    internal val block: CidrBlock get() = CidrBlock(bits, prefixLength)

    /**
     * The address of the network: its first address, with every host bit cleared, such as `192.0.2.0` for
     * `192.0.2.0/24`.
     *
     * @since 1.0.0
     */
    public abstract val address: IpAddress

    /**
     * The netmask of [prefixLength], such as `255.255.255.0` for `/24`.
     *
     * @since 1.0.0
     */
    public abstract val netmask: Netmask

    /**
     * The wildcard mask, the inverse of [netmask] with every host bit set, such as `0.0.0.255` for `/24`.
     *
     * @since 1.0.0
     */
    public abstract val wildcard: IpAddress

    /**
     * The first address of the network, which equals [address].
     *
     * @since 1.0.0
     */
    public abstract val firstAddress: IpAddress

    /**
     * The last address of the network, with every host bit set, such as `192.0.2.255` for `192.0.2.0/24`.
     *
     * @since 1.0.0
     */
    public abstract val lastAddress: IpAddress

    /**
     * The first address a host can use.
     *
     * In an IPv4 network of `/30` or shorter, it is the address after [address]; in an IPv4 `/31` or `/32` and in
     * every IPv6 network, it is [firstAddress].
     *
     * @since 1.0.0
     */
    public abstract val firstHost: IpAddress

    /**
     * The last address a host can use.
     *
     * In an IPv4 network of `/30` or shorter, it is the address before the broadcast address; in an IPv4 `/31` or
     * `/32` and in every IPv6 network, it is [lastAddress].
     *
     * @since 1.0.0
     */
    public abstract val lastHost: IpAddress

    /**
     * The number of addresses from [firstHost] to [lastHost], exact up to `2^128`.
     *
     * It is [addressCount] less two in an IPv4 network of `/30` or shorter, such as `254` for a `/24`, and
     * [addressCount] in an IPv4 `/31` or `/32` and in every IPv6 network.
     *
     * @since 1.0.0
     */
    public abstract val hostCount: AddressCount

    /**
     * The number of addresses in this network, `2` to the power of the number of host bits, such as `256` for an
     * IPv4 `/24` and `2^128` for `::/0`; it is exact on every platform.
     *
     * @since 1.0.0
     */
    public val addressCount: AddressCount get() = AddressCount.ofPowerOfTwo(hostBits)

    /**
     * Tells whether [address] lies in this network.
     *
     * @param address the address to look for.
     * @return `true` if [address] has the version of this network and shares its first [prefixLength] bits;
     * `false` for an address of the other version.
     * @since 1.0.0
     */
    public operator fun contains(address: IpAddress): Boolean =
        address.version == version && address.bits intersect prefixMask(version, prefixLength) == bits

    /**
     * Tells whether every address of [network] lies in this network.
     *
     * A network contains itself and each of its subnets, and never a network of the other version.
     *
     * @param network the network to look for.
     * @return `true` if [network] has the version of this network, a prefix length not shorter than [prefixLength]
     * and an address that lies in this network.
     * @since 1.0.0
     */
    public operator fun contains(network: IpNetwork): Boolean =
        network.version == version && network.prefixLength >= prefixLength && network.address in this

    /**
     * Tells whether this network and [network] share at least one address.
     *
     * Two networks in CIDR notation share an address only when one of them contains the other, so this is `true`
     * exactly when either contains the other. Networks of different versions never overlap.
     *
     * @param network the other network.
     * @return `true` if the two networks have an address in common.
     * @since 1.0.0
     */
    public fun overlaps(network: IpNetwork): Boolean = network in this || this in network

    internal fun requireSubnetPrefixLength(subnetPrefixLength: Int) {
        require(subnetPrefixLength in prefixLength..version.bitLength) {
            val range = "$prefixLength..${version.bitLength}"
            "A subnet of $this needs a prefix length in $range, but got $subnetPrefixLength"
        }
    }

    internal fun requireSupernetPrefixLength(supernetPrefixLength: Int) {
        require(supernetPrefixLength in 0..prefixLength) {
            "A supernet of $this needs a prefix length in 0..$prefixLength, but got $supernetPrefixLength"
        }
    }

    /**
     * Reports a [prefixLength] outside `0..32` for IPv4 or outside `0..128` for IPv6 at `prefixLength` with the code
     * [IpViolationCodes.INVALID_PREFIX], and otherwise an [address] with host bits set at `address` with the code
     * [IpViolationCodes.HOST_BITS_SET].
     *
     * The message of the second names the network address that the address would have with its host bits cleared,
     * such as `192.0.2.0`, so a caller can tell what was meant.
     *
     * @receiver the validation that collects the violation.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (!checkPrefixLength(::prefixLength, prefixLength, version)) return
        if (hasHostBits(bits, version, prefixLength)) {
            val network = addressText(bits intersect prefixMask(version, prefixLength), version)
            fail(
                property = ::address,
                message = "has host bits set beyond the prefix length $prefixLength, the network is $network",
                code = IpViolationCodes.HOST_BITS_SET,
            )
        }
    }

    /**
     * Compares this network with [other] by their addresses, IPv4 before IPv6, and then by their prefix lengths, so
     * `192.0.2.0/24` comes before `192.0.2.0/25`.
     *
     * @param other the network to compare with.
     * @return a negative number, zero or a positive number as this network is below, equal to or above [other].
     * @since 1.0.0
     */
    final override fun compareTo(other: IpNetwork): Int {
        val byAddress = address.compareTo(other.address)
        if (byAddress != 0) return byAddress
        return prefixLength.compareTo(other.prefixLength)
    }

    /**
     * Tells whether [other] is a network with the same address and the same prefix length.
     *
     * A network is never equal to an [IpInterface], even when both show the same text.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal network.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean =
        other is IpNetwork && address == other.address && prefixLength == other.prefixLength

    /**
     * Computes a hash code from the address and the prefix length.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = HASH_FACTOR * address.hashCode() + prefixLength

    /**
     * Shows this network as its address and its prefix length, such as `192.0.2.0/24` or `2001:db8::/32`.
     *
     * @return the network in the canonical form, which `parse` reads.
     * @since 1.0.0
     */
    final override fun toString(): String = "$address/$prefixLength"

    /**
     * The reading of networks of either version from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as a network of either version.
         *
         * A text that holds a colon is read as [Ipv6Network.parse] reads it, every other text as
         * [Ipv4Network.parse] reads it. A network with host bits set is refused, never masked. A text longer than
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network.
         * @throws IllegalArgumentException if [text] is not a network. The message names the version it was read as
         * and the problem in fixed words, such as `Not an IPv4 network: it has host bits set beyond its prefix
         * length, the network is 192.0.2.0/24`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): IpNetwork {
            if (isIpv6Text(text)) return Ipv6Network.parse(text)
            return Ipv4Network.parse(text)
        }

        /**
         * Reads [text] as a network of either version, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the network, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpNetwork? {
            if (isIpv6Text(text)) return Ipv6Network.parseOrNull(text)
            return Ipv4Network.parseOrNull(text)
        }
    }
}
