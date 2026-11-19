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

package com.davils.konnect.core.port

import com.davils.konnect.core.port.grammar.PortRangeReading
import com.davils.konnect.core.port.grammar.readPortRange
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "port range"
private const val HASH_FACTOR: Int = 31

/**
 * A contiguous range of ports from [first] to [last], both included.
 *
 * A range holds at least one port. It is iterable over its ports in ascending order, and [toString] shows it as
 * `8000-8080`, or as the single port, such as `443`, when it holds one port only. Instances are immutable.
 *
 * @property first the lowest port of the range.
 * @property last the highest port of the range; never below [first].
 * @since 1.0.0
 */
public class PortRange private constructor(
    public val first: Port,
    public val last: Port,
) : Iterable<Port>, Validatable {
    /**
     * The number of ports in this range, from `1` to `65536`.
     *
     * @since 1.0.0
     */
    public val size: Int get() = last.value - first.value + 1

    /**
     * Tells whether [port] lies in this range.
     *
     * @param port the port to look for.
     * @return `true` if [port] is neither below [first] nor above [last].
     * @since 1.0.0
     */
    public operator fun contains(port: Port): Boolean = port in first..last

    /**
     * Tells whether every port of [range] lies in this range.
     *
     * @param range the range to look for.
     * @return `true` if [range] lies completely inside this range.
     * @since 1.0.0
     */
    public operator fun contains(range: PortRange): Boolean = range.first in this && range.last in this

    /**
     * Tells whether this range and [range] share at least one port.
     *
     * @param range the other range.
     * @return `true` if the two ranges have a port in common.
     * @since 1.0.0
     */
    public fun overlaps(range: PortRange): Boolean = range.first <= last && first <= range.last

    /**
     * Iterates over the ports of this range in ascending order.
     *
     * @return an iterator over at most `65536` ports, created lazily.
     * @since 1.0.0
     */
    override fun iterator(): Iterator<Port> {
        val values = (first.value..last.value).asSequence()
        return values.map(::Port).iterator()
    }

    /**
     * Reports a [last] port below the [first] with the code [PortViolationCodes.INVALID_PORT_RANGE].
     *
     * @receiver the validation that collects the violation.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (first > last) {
            fail(
                property = ::last,
                message = "must not be below the first port $first, but was $last",
                code = PortViolationCodes.INVALID_PORT_RANGE,
            )
        }
    }

    /**
     * Tells whether [other] is a port range with the same first and last port.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal port range.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is PortRange && first == other.first && last == other.last

    /**
     * Computes a hash code from the first and the last port.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * first.hashCode() + last.hashCode()

    /**
     * Shows this range as `8000-8080`, or as the single port, such as `443`, when [first] equals [last].
     *
     * @return the range in the form [parse] reads.
     * @since 1.0.0
     */
    override fun toString(): String {
        if (first == last) return first.toString()
        return "$first-$last"
    }

    /**
     * The range of all ports, the creation of ranges and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The range of all ports, `0-65535`.
         *
         * @since 1.0.0
         */
        public val ALL: PortRange = PortRange(first = Port.MIN, last = Port.MAX)

        /**
         * Creates the range from [first] to [last], both included.
         *
         * @param first the lowest port of the range.
         * @param last the highest port of the range.
         * @return the range.
         * @throws com.davils.kore.core.validation.report.ValidationException if [last] is below [first]; the
         * report holds one error at `last` with the code [PortViolationCodes.INVALID_PORT_RANGE].
         * @since 1.0.0
         */
        public operator fun invoke(first: Port, last: Port): PortRange = PortRange(first, last).validateOrThrow(SUBJECT)

        /**
         * Reads [text] as a port range.
         *
         * The text is a single port, such as `443`, or two ports joined by a dash, such as `8000-8080`; spaces
         * around the dash are allowed. Each port is read as [Port.parse] reads it, and the first must not be above
         * the last. A text longer than
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range.
         * @throws IllegalArgumentException if [text] is not a port range. The message names the problem in fixed
         * words, such as `Not a port range: it starts after it ends`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): PortRange = when (val reading = readPortRange(text)) {
            is PortRangeReading.Read -> fromReading(reading)
            is PortRangeReading.Refused -> throw IllegalArgumentException("Not a port range: it ${reading.reason}")
        }

        /**
         * Reads [text] as a port range, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): PortRange? = when (val reading = readPortRange(text)) {
            is PortRangeReading.Read -> fromReading(reading)
            is PortRangeReading.Refused -> null
        }

        internal fun assembled(first: Port, last: Port): PortRange = PortRange(first, last)

        internal fun fromReading(reading: PortRangeReading.Read): PortRange = PortRange(
            first = Port(reading.first),
            last = Port(reading.last),
        )
    }
}
