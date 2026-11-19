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

package com.davils.konnect.internetprotocol.mask

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.address.bits.hostMask
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.mask.reading.MaskReading
import com.davils.konnect.internetprotocol.mask.reading.readMask

private val VERSION: IpVersion = IpVersion.V6

/**
 * A netmask of IPv6, such as `ffff:ffff:ffff:ffff::`, with a prefix length from `0` to `128`.
 *
 * A netmask is created from its prefix length with [ofPrefix] or read from its address form with [parse]. Its [address]
 * and its [wildcard] are [Ipv6Address]es. Instances are immutable.
 *
 * @property prefixLength the number of leading ones, from `0` to `128`.
 * @since 1.0.0
 */
public class Ipv6Netmask private constructor(override val prefixLength: Int) : Netmask() {
    /**
     * The version of this netmask, always [IpVersion.V6].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = VERSION

    /**
     * This netmask as an IPv6 address, such as `ffff:ffff:ffff:ffff::` for the prefix length `64`.
     *
     * @since 1.0.0
     */
    override val address: Ipv6Address get() = Ipv6Address.fromBits(prefixMask(VERSION, prefixLength))

    /**
     * The inverse of this netmask as an IPv6 address, with the host bits set, such as `::ffff:ffff:ffff:ffff` for the
     * prefix length `64`.
     *
     * @since 1.0.0
     */
    override val wildcard: Ipv6Address get() = Ipv6Address.fromBits(hostMask(VERSION, prefixLength))

    /**
     * The creation of IPv6 netmasks from a prefix length and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates the IPv6 netmask with [prefixLength] leading ones.
         *
         * @param prefixLength the number of leading ones, from `0` to `128`.
         * @return the netmask.
         * @throws IllegalArgumentException if [prefixLength] is outside `0..128`.
         * @since 1.0.0
         */
        public fun ofPrefix(prefixLength: Int): Ipv6Netmask {
            require(prefixLength in 0..VERSION.bitLength) {
                "An IPv6 prefix length must be in 0..${VERSION.bitLength}, but was $prefixLength"
            }
            return Ipv6Netmask(prefixLength)
        }

        /**
         * Reads [text] as an IPv6 netmask in its address form, such as `ffff:ffff:ffff:ffff::`.
         *
         * The text must be an IPv6 address as [Ipv6Address.parse] reads it: eight groups of one to four hexadecimal
         * digits in either case joined by colons, with `::` at most once for a run of zero groups and optionally an
         * embedded IPv4 address at the end, without a zone index and without whitespace. The bits of the address must
         * be a run of ones followed by a run of zeros; a netmask whose ones are not contiguous is refused. A prefix
         * length, such as `64` or `/64`, is not read here; [ofPrefix] creates a netmask from one. A text longer than
         * `45` characters, the longest form of an IPv6 address, is refused before it is read, which keeps every text
         * above [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] out as well.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the netmask.
         * @throws IllegalArgumentException if [text] is not an IPv6 netmask. The message names the problem in fixed
         * words, such as `Not an IPv6 netmask: it has a one after a zero, so its ones are not contiguous`, and never
         * quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv6Netmask = when (val reading = readMask(text, VERSION)) {
            is MaskReading.Read -> Ipv6Netmask(reading.prefixLength)
            is MaskReading.Refused -> throw IllegalArgumentException("Not an IPv6 netmask: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv6 netmask, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the netmask, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv6Netmask? = when (val reading = readMask(text, VERSION)) {
            is MaskReading.Read -> Ipv6Netmask(reading.prefixLength)
            is MaskReading.Refused -> null
        }
    }
}
