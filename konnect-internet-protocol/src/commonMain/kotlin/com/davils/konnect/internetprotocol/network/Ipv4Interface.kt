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
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.bits.prefixMask
import com.davils.konnect.internetprotocol.mask.Ipv4Netmask
import com.davils.konnect.internetprotocol.subnet.grammar.CidrReading
import com.davils.konnect.internetprotocol.subnet.grammar.readCidr
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "interface"
private val VERSION: IpVersion = IpVersion.V4

/**
 * An IPv4 address of a host together with the prefix length of the network it is attached to, such as `192.0.2.10/24`.
 *
 * The address may have host bits set; [network] is the network it lies in. An interface is created with [invoke], read
 * with [parse] or built with [ipv4Interface][com.davils.konnect.internetprotocol.subnet.ipv4Interface]. Instances are
 * immutable.
 *
 * @property address the address of the host, which may have host bits set.
 * @property prefixLength the length of the prefix of the network, from `0` to `32`.
 * @since 1.0.0
 */
public class Ipv4Interface private constructor(
    override val address: Ipv4Address,
    override val prefixLength: Int,
) : IpInterface() {
    /**
     * The version of this interface, always [IpVersion.V4].
     *
     * @since 1.0.0
     */
    override val version: IpVersion get() = VERSION

    /**
     * The netmask of [prefixLength], such as `255.255.255.0` for `/24`.
     *
     * @since 1.0.0
     */
    override val netmask: Ipv4Netmask get() = Ipv4Netmask.ofPrefix(prefixLength)

    /**
     * The network that [address] lies in: [address] with every host bit cleared, at the same [prefixLength], such as
     * `192.0.2.0/24` for the interface `192.0.2.10/24`.
     *
     * @since 1.0.0
     */
    override val network: Ipv4Network
        get() = Ipv4Network.fromBits(address.bits intersect prefixMask(VERSION, prefixLength), prefixLength)

    /**
     * The creation of IPv4 interfaces and their reading from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates the interface of [address] with [prefixLength].
         *
         * @param address the address of the host; it may have host bits set.
         * @param prefixLength the length of the prefix of the network, from `0` to `32`.
         * @return the interface.
         * @throws com.davils.kore.core.validation.report.ValidationException if [prefixLength] is outside `0..32`; the
         * report holds one error at `prefixLength` with the code
         * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX].
         * @since 1.0.0
         */
        public operator fun invoke(address: Ipv4Address, prefixLength: Int): Ipv4Interface =
            Ipv4Interface(address, prefixLength).validateOrThrow(SUBJECT)

        /**
         * Reads [text] as an IPv4 interface.
         *
         * The text is an IPv4 address as [Ipv4Address.parse] reads it, four decimal numbers from `0` to `255` without
         * leading zeros, optionally followed by a slash and either a prefix length from `0` to `32` in decimal without
         * leading zeros, such as `192.0.2.10/24`, or a netmask in dotted-quad form whose ones are contiguous, such as
         * `192.0.2.10/255.255.255.0`. Without a slash, the prefix length is `32`. The address may have host bits set.
         * No whitespace is allowed anywhere, and a text longer than
         * [Limits.MAX_TEXT_LENGTH][com.davils.konnect.core.Limits.MAX_TEXT_LENGTH] is refused before it is read.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the interface.
         * @throws IllegalArgumentException if [text] is not an IPv4 interface. The message names the problem in fixed
         * words, such as `Not an IPv4 interface: it has a prefix length that is not a number in 0..32`, and never
         * quotes [text].
         * @since 1.0.0
         */
        public fun parse(text: String): Ipv4Interface = when (val reading = readCidr(text, VERSION)) {
            is CidrReading.Read -> fromReading(reading)
            is CidrReading.Refused -> throw IllegalArgumentException("Not an IPv4 interface: it ${reading.reason}")
        }

        /**
         * Reads [text] as an IPv4 interface, as [parse] does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the interface, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Ipv4Interface? = when (val reading = readCidr(text, VERSION)) {
            is CidrReading.Read -> fromReading(reading)
            is CidrReading.Refused -> null
        }

        internal fun assembled(address: Ipv4Address, prefixLength: Int): Ipv4Interface =
            Ipv4Interface(address, prefixLength)

        private fun fromReading(reading: CidrReading.Read): Ipv4Interface =
            Ipv4Interface(Ipv4Address.fromBits(reading.bits), reading.prefixLength)
    }
}
