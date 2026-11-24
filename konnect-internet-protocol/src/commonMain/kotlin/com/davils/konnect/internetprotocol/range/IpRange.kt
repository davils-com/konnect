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

package com.davils.konnect.internetprotocol.range

import com.davils.konnect.internetprotocol.AddressCount
import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.grammar.isIpv6Text
import com.davils.konnect.internetprotocol.network.IpNetwork
import com.davils.konnect.internetprotocol.network.arithmetic.Interval
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val HASH_FACTOR: Int = 31

/**
 * A contiguous range of IPv4 or IPv6 addresses from [first] to [last], both included.
 *
 * A range is either an [Ipv4Range] or an [Ipv6Range] and holds at least one address. Unlike a network, its ends need
 * not lie on the boundaries of a prefix; [toNetworks] covers it with networks. Two ranges are equal when they have the
 * same first and last address, and [toString] shows a range as `192.0.2.10-192.0.2.20`, also when it holds a single
 * address. Instances are immutable.
 *
 * @since 1.0.0
 */
public sealed class IpRange : Validatable {
    /**
     * The version of the internet protocol of both ends of this range.
     *
     * @since 1.0.0
     */
    public abstract val version: IpVersion

    /**
     * The lowest address of the range.
     *
     * @since 1.0.0
     */
    public abstract val first: IpAddress

    /**
     * The highest address of the range; never before [first].
     *
     * @since 1.0.0
     */
    public abstract val last: IpAddress

    /**
     * The number of addresses in this range, from `1` to `2^32` for IPv4 and from `1` to `2^128` for IPv6.
     *
     * The count is exact, also for the range of all IPv6 addresses.
     *
     * @since 1.0.0
     */
    public val size: AddressCount get() = AddressCount.ofSpan(first = first.bits, last = last.bits)

    internal val interval: Interval get() = Interval(first = first.bits, last = last.bits)

    /**
     * Covers this range with the fewest networks that hold exactly its addresses.
     *
     * Each network is the largest one that starts at the first address not yet covered and does not reach beyond
     * [last]. The networks are in ascending order, do not overlap and together hold every address of the range and no
     * other, so a range that is a network gives that one network. There are at most `62` networks for IPv4 and `254`
     * for IPv6.
     *
     * @return the networks, in ascending order; never empty.
     * @since 1.0.0
     */
    public abstract fun toNetworks(): List<IpNetwork>

    /**
     * Tells whether [address] lies in this range.
     *
     * @param address the address to look for.
     * @return `true` if [address] has the [version] of this range and is neither before [first] nor after [last]; an
     * address of the other version is never in the range.
     * @since 1.0.0
     */
    public operator fun contains(address: IpAddress): Boolean =
        address.version == version && address >= first && address <= last

    /**
     * Tells whether every address of [range] lies in this range.
     *
     * @param range the range to look for.
     * @return `true` if [range] lies completely inside this range; a range of the other version never does.
     * @since 1.0.0
     */
    public operator fun contains(range: IpRange): Boolean = range.first in this && range.last in this

    /**
     * Tells whether this range and [range] share at least one address.
     *
     * @param range the other range.
     * @return `true` if the two ranges have an address in common; ranges of different versions never do.
     * @since 1.0.0
     */
    public fun overlaps(range: IpRange): Boolean =
        range.version == version && range.first <= last && first <= range.last

    /**
     * Reports a [last] address before the [first] with the code [IpViolationCodes.INVALID_RANGE].
     *
     * @receiver the validation that collects the violation.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (first > last) {
            fail(
                property = ::last,
                message = "must not be before the first address $first, but was $last",
                code = IpViolationCodes.INVALID_RANGE,
            )
        }
    }

    /**
     * Tells whether [other] is a range with the same first and last address.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal range; an IPv4 and an IPv6 range are never equal.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean = other is IpRange && first == other.first && last == other.last

    /**
     * Computes a hash code from the first and the last address.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = HASH_FACTOR * first.hashCode() + last.hashCode()

    /**
     * Shows this range as its first and its last address joined by a dash, such as `192.0.2.10-192.0.2.20` or
     * `2001:db8::10-2001:db8::20`.
     *
     * Each address is shown in its canonical form, and a range of a single address shows it twice, such as
     * `192.0.2.1-192.0.2.1`.
     *
     * @return the range in the form [parse] reads.
     * @since 1.0.0
     */
    final override fun toString(): String = "$first-$last"

    /**
     * The reading of ranges of either version from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] as an IPv4 or an IPv6 range.
         *
         * A text that holds a colon is read as [Ipv6Range.parse] reads it, and every other text as [Ipv4Range.parse]
         * reads it, so a range whose ends have different versions is refused. A text longer than
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range, an [Ipv4Range] or an [Ipv6Range].
         * @throws IllegalArgumentException if [text] is not a range of the version it is read as. The message names the
         * problem in fixed words, such as `Not an IPv4 range: it starts after it ends`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): IpRange {
            if (isIpv6Text(text)) return Ipv6Range.parse(text)
            return Ipv4Range.parse(text)
        }

        /**
         * Reads [text] as an IPv4 or an IPv6 range, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpRange? {
            if (isIpv6Text(text)) return Ipv6Range.parseOrNull(text)
            return Ipv4Range.parseOrNull(text)
        }
    }
}
