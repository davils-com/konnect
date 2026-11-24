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

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.network.Ipv4Network
import com.davils.konnect.internetprotocol.network.arithmetic.cidrBlocks
import com.davils.konnect.internetprotocol.range.grammar.RangeReading
import com.davils.konnect.internetprotocol.range.grammar.readRange
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "range"
private val VERSION: IpVersion = IpVersion.V4

/**
 * A contiguous range of IPv4 addresses from [first] to [last], both included, such as `192.0.2.10-192.0.2.20`.
 *
 * A range holds at least one address. It is created with [invoke], read from text with [parse] or built with
 * [ipv4Range][com.davils.konnect.internetprotocol.range.ipv4Range]. Instances are immutable.
 *
 * @property first the lowest address of the range.
 * @property last the highest address of the range; never before [first].
 * @since 1.0.0
 */
public class Ipv4Range private constructor(
    override val first: Ipv4Address,
    override val last: Ipv4Address,
) : IpRange() {
    /**
     * The version of this range, always [IpVersion.V4].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = VERSION

    /**
     * Covers this range with the fewest IPv4 networks that hold exactly its addresses, as [IpRange.toNetworks]
     * describes.
     *
     * @return at most `62` networks, in ascending order; never empty.
     * @since 1.0.0
     */
    override fun toNetworks(): List<Ipv4Network> {
        val blocks = cidrBlocks(interval, VERSION)
        return blocks.map { block -> Ipv4Network.fromBits(block.bits, block.prefixLength) }
    }

    /**
     * The creation of IPv4 ranges and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates the range from [first] to [last], both included.
         *
         * @param first the lowest address of the range.
         * @param last the highest address of the range.
         * @return the range.
         * @throws com.davils.kore.core.validation.report.ValidationException if [last] is before [first]; the report
         * holds one error at `last` with the code
         * [IpViolationCodes.INVALID_RANGE][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_RANGE].
         * @since 1.0.0
         */
        public operator fun invoke(first: Ipv4Address, last: Ipv4Address): Ipv4Range =
            Ipv4Range(first, last).validateOrThrow(SUBJECT)

        /**
         * Reads [text] as an IPv4 range.
         *
         * The text is a single address, such as `192.0.2.1`, which is read as the range of that one address, or two
         * addresses joined by a dash, such as `192.0.2.10-192.0.2.20`. Each address is read as [Ipv4Address.parse]
         * reads it. Whitespace before and after each address is dropped, so `192.0.2.10 - 192.0.2.20` is read as well,
         * and the first address must not be after the last. A text with more than one dash is refused. A text longer
         * than [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is split.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range.
         * @throws IllegalArgumentException if [text] is not an IPv4 range. The message names the problem in fixed
         * words, such as `Not an IPv4 range: it starts after it ends`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv4Range = when (val reading = readRange(text, VERSION)) {
            is RangeReading.Read -> fromReading(reading)
            is RangeReading.Refused -> throw IllegalArgumentException("Not an IPv4 range: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv4 range, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv4Range? = when (val reading = readRange(text, VERSION)) {
            is RangeReading.Read -> fromReading(reading)
            is RangeReading.Refused -> null
        }

        internal fun assembled(first: Ipv4Address, last: Ipv4Address): Ipv4Range = Ipv4Range(first, last)

        private fun fromReading(reading: RangeReading.Read): Ipv4Range = Ipv4Range(
            first = Ipv4Address.fromBits(reading.first),
            last = Ipv4Address.fromBits(reading.last),
        )
    }
}
