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

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.grammar.isIpv6Text
import com.davils.konnect.internetprotocol.mask.Netmask
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val HASH_FACTOR: Int = 31

/**
 * The address of a host together with the prefix length of the network it is attached to, such as `192.0.2.10/24`
 * or `2001:db8::10/64`.
 *
 * Unlike an [IpNetwork], an interface may have host bits set: its [address] is the address of the host, and
 * [network] is the network that address lies in. An interface is either an [Ipv4Interface] or an [Ipv6Interface].
 * Two interfaces are equal when they have the same address and the same prefix length. They are ordered by their
 * address, IPv4 before IPv6, and then by their prefix length, and [toString] shows them as `192.0.2.10/24`.
 * Instances are immutable.
 *
 * @since 1.0.0
 */
public sealed class IpInterface : Comparable<IpInterface>, Validatable {
    /**
     * The version of the Internet Protocol of this interface.
     *
     * @since 1.0.0
     */
    public abstract val version: IpVersion

    /**
     * The address of the host, which may have host bits set.
     *
     * @since 1.0.0
     */
    public abstract val address: IpAddress

    /**
     * The length of the prefix of the network, from `0` to `32` for IPv4 and from `0` to `128` for IPv6.
     *
     * @since 1.0.0
     */
    public abstract val prefixLength: Int

    /**
     * The netmask of [prefixLength], such as `255.255.255.0` for `/24`.
     *
     * @since 1.0.0
     */
    public abstract val netmask: Netmask

    /**
     * The network that [address] lies in: [address] with every host bit cleared, at the same [prefixLength], such as
     * `192.0.2.0/24` for the interface `192.0.2.10/24`.
     *
     * @since 1.0.0
     */
    public abstract val network: IpNetwork

    /**
     * Reports a [prefixLength] outside `0..32` for IPv4 or outside `0..128` for IPv6 at `prefixLength` with the code
     * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX].
     *
     * @receiver the validation that collects the violation.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkPrefixLength(::prefixLength, prefixLength, version)
    }

    /**
     * Compares this interface with [other] by their addresses, IPv4 before IPv6, and then by their prefix lengths.
     *
     * @param other the interface to compare with.
     * @return a negative number, zero or a positive number as this interface is below, equal to or above [other].
     * @since 1.0.0
     */
    final override fun compareTo(other: IpInterface): Int {
        val byAddress = address.compareTo(other.address)
        if (byAddress != 0) return byAddress
        return prefixLength.compareTo(other.prefixLength)
    }

    /**
     * Tells whether [other] is an interface with the same address and the same prefix length.
     *
     * An interface is never equal to an [IpNetwork], even when both show the same text.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal interface.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean =
        other is IpInterface && address == other.address && prefixLength == other.prefixLength

    /**
     * Computes a hash code from the address and the prefix length.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = HASH_FACTOR * address.hashCode() + prefixLength

    /**
     * Shows this interface as its address and its prefix length, such as `192.0.2.10/24` or `2001:db8::10/64`.
     *
     * @return the interface in the canonical form, which `parse` reads.
     * @since 1.0.0
     */
    final override fun toString(): String = "$address/$prefixLength"

    /**
     * The reading of interfaces of either version from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as an interface of either version.
         *
         * A text that holds a colon is read as [Ipv6Interface.parse] reads it, every other text as
         * [Ipv4Interface.parse] reads it. A text longer than
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the interface.
         * @throws IllegalArgumentException if [text] is not an interface. The message names the version it was read
         * as and the problem in fixed words, such as `Not an IPv4 interface: it has a prefix length that is not a
         * number in 0..32`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): IpInterface {
            if (isIpv6Text(text)) return Ipv6Interface.parse(text)
            return Ipv4Interface.parse(text)
        }

        /**
         * Reads [text] as an interface of either version, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the interface, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpInterface? {
            if (isIpv6Text(text)) return Ipv6Interface.parseOrNull(text)
            return Ipv4Interface.parseOrNull(text)
        }
    }
}
