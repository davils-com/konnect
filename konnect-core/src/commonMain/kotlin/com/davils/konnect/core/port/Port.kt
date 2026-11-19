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

import com.davils.konnect.core.port.grammar.PORT_MAX
import com.davils.konnect.core.port.grammar.PortReading
import com.davils.konnect.core.port.grammar.readPort
import kotlin.jvm.JvmInline

/**
 * A port of TCP, UDP, SCTP or DCCP: a number from `0` to `65535`.
 *
 * A port is a value class over its number and costs no allocation where it is not boxed. Ports are ordered by their
 * number, and [toString] shows the number in decimal.
 *
 * @property value the number of the port, from `0` to `65535`.
 * @constructor Creates the port with the number [value].
 * @throws IllegalArgumentException if [value] is outside `0..65535`.
 * @since 1.0.0
 */
@JvmInline
public value class Port(public val value: Int) : Comparable<Port> {
    init {
        require(value in 0..PORT_MAX) { "A port must be in 0..$PORT_MAX, but was $value" }
    }

    /**
     * The range of RFC 6335 that this port belongs to.
     *
     * @since 1.0.0
     */
    public val category: PortCategory get() = PortCategory.forPort(value)

    /**
     * Compares this port with [other] by their numbers.
     *
     * @param other the port to compare with.
     * @return a negative number, zero or a positive number as this port is below, equal to or above [other].
     * @since 1.0.0
     */
    override fun compareTo(other: Port): Int = value.compareTo(other.value)

    /**
     * Shows the number of this port in decimal, such as `443`.
     *
     * @return the number in decimal.
     * @since 1.0.0
     */
    override fun toString(): String = value.toString()

    /**
     * The lowest and the highest port and the reading of ports from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The lowest port, `0`.
         *
         * @since 1.0.0
         */
        public val MIN: Port = Port(0)

        /**
         * The highest port, `65535`.
         *
         * @since 1.0.0
         */
        public val MAX: Port = Port(PORT_MAX)

        /**
         * Reads [text] as a port.
         *
         * The text must be a decimal number from `0` to `65535` without a sign, without leading zeros and without
         * whitespace. A text longer than [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH]
         * is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the port.
         * @throws IllegalArgumentException if [text] is not a port. The message names the problem in fixed words,
         * such as `Not a port: it has a leading zero`, and never quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Port = when (val reading = readPort(text)) {
            is PortReading.Read -> Port(reading.value)
            is PortReading.Refused -> throw IllegalArgumentException("Not a port: it ${reading.reason}")
        }

        /**
         * Reads [text] as a port, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the port, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Port? = when (val reading = readPort(text)) {
            is PortReading.Read -> Port(reading.value)
            is PortReading.Refused -> null
        }
    }
}
