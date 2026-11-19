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

package com.davils.konnect.internetprotocol

import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.address.grammar.AddressReading
import com.davils.konnect.internetprotocol.address.grammar.isIpv6Text
import com.davils.konnect.internetprotocol.address.grammar.readIpv4
import com.davils.konnect.internetprotocol.address.grammar.readIpv6
import com.davils.konnect.internetprotocol.range.Ipv4Range
import com.davils.konnect.internetprotocol.range.Ipv6Range
import com.davils.konnect.internetprotocol.range.grammar.RangeReading
import com.davils.konnect.internetprotocol.range.grammar.readRange
import com.davils.konnect.internetprotocol.subnet.Ipv4Interface
import com.davils.konnect.internetprotocol.subnet.Ipv4Network
import com.davils.konnect.internetprotocol.subnet.Ipv6Interface
import com.davils.konnect.internetprotocol.subnet.Ipv6Network
import com.davils.konnect.internetprotocol.subnet.grammar.CidrReading
import com.davils.konnect.internetprotocol.subnet.grammar.readCidr
import com.davils.konnect.internetprotocol.subnet.grammar.readNetwork
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

/**
 * Reads [text] as an IPv4 or an IPv6 address inside a validation, reporting a violation at [property] instead of
 * throwing.
 *
 * A text that holds a colon is read as [ipv6Address] reads it, every other text as [ipv4Address] reads it, with the
 * same limits. A text that is not an address is reported as an error with the code
 * [IpViolationCodes.INVALID_ADDRESS]. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the address, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipAddress(property: KProperty<*>, text: String): IpAddress? {
    if (isIpv6Text(text)) return ipv6Address(property, text)
    return ipv4Address(property, text)
}

/**
 * Reads [text] as an IPv4 address inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv4Address.parse] reads it, with the same limits. A text that is not an IPv4 address is
 * reported as an error with the code [IpViolationCodes.INVALID_ADDRESS] and a message such as `is not an IPv4
 * address: it has a number with a leading zero`. The message names the problem in fixed words and never quotes
 * [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the address, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv4Address(property: KProperty<*>, text: String): Ipv4Address? =
    when (val reading = readIpv4(text)) {
        is AddressReading.Read -> Ipv4Address.fromBits(reading.bits)

        is AddressReading.Refused -> refuse(
            property = property,
            message = "is not an IPv4 address: it ${reading.reason}",
            code = IpViolationCodes.INVALID_ADDRESS,
        )
    }

/**
 * Reads [text] as an IPv6 address inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv6Address.parse] reads it, with the same limits. A text that is not an IPv6 address is
 * reported as an error with the code [IpViolationCodes.INVALID_ADDRESS] and a message such as `is not an IPv6
 * address: it has a zone index, which is not supported`. The message names the problem in fixed words and never
 * quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the address, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv6Address(property: KProperty<*>, text: String): Ipv6Address? =
    when (val reading = readIpv6(text)) {
        is AddressReading.Read -> Ipv6Address.fromBits(reading.bits)

        is AddressReading.Refused -> refuse(
            property = property,
            message = "is not an IPv6 address: it ${reading.reason}",
            code = IpViolationCodes.INVALID_ADDRESS,
        )
    }

/**
 * Reads [text] as an IPv4 network inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv4Network.parse] reads it, with the same limits: an IPv4 address, optionally followed by
 * `/` and a prefix length or a contiguous netmask, such as `192.0.2.0/24` or `192.0.2.0/255.255.255.0`, whose
 * address has no bits set beyond the prefix length. A text that is not an IPv4 network is reported as an error with
 * the code [IpViolationCodes.INVALID_NETWORK]. The message names the problem in fixed words and never quotes [text];
 * for an address with host bits set it names the network that the prefix length would give, in its canonical form.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the network, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv4Network(property: KProperty<*>, text: String): Ipv4Network? =
    when (val reading = readNetwork(text, IpVersion.V4)) {
        is CidrReading.Read -> Ipv4Network.fromBits(reading.bits, reading.prefixLength)

        is CidrReading.Refused -> refuse(
            property = property,
            message = "is not an IPv4 network: it ${reading.reason}",
            code = IpViolationCodes.INVALID_NETWORK,
        )
    }

/**
 * Reads [text] as an IPv6 network inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv6Network.parse] reads it, with the same limits: an IPv6 address, optionally followed by
 * `/` and a prefix length, such as `2001:db8::/32`, whose address has no bits set beyond the prefix length. A text
 * that is not an IPv6 network is reported as an error with the code [IpViolationCodes.INVALID_NETWORK]. The message
 * names the problem in fixed words and never quotes [text]; for an address with host bits set it names the network
 * that the prefix length would give, in its canonical form.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the network, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv6Network(property: KProperty<*>, text: String): Ipv6Network? =
    when (val reading = readNetwork(text, IpVersion.V6)) {
        is CidrReading.Read -> Ipv6Network.fromBits(reading.bits, reading.prefixLength)

        is CidrReading.Refused -> refuse(
            property = property,
            message = "is not an IPv6 network: it ${reading.reason}",
            code = IpViolationCodes.INVALID_NETWORK,
        )
    }

/**
 * Reads [text] as an IPv4 interface inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv4Interface.parse] reads it, with the same limits: an IPv4 address, optionally followed by
 * `/` and a prefix length or a contiguous netmask, such as `192.0.2.1/24`; unlike a network, the address may have
 * bits set beyond the prefix length. A text that is not an IPv4 interface is reported as an error with the code
 * [IpViolationCodes.INVALID_INTERFACE]. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the interface, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv4Interface(property: KProperty<*>, text: String): Ipv4Interface? =
    when (val reading = readCidr(text, IpVersion.V4)) {
        is CidrReading.Read -> Ipv4Interface.assembled(Ipv4Address.fromBits(reading.bits), reading.prefixLength)

        is CidrReading.Refused -> refuse(
            property = property,
            message = "is not an IPv4 interface: it ${reading.reason}",
            code = IpViolationCodes.INVALID_INTERFACE,
        )
    }

/**
 * Reads [text] as an IPv6 interface inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv6Interface.parse] reads it, with the same limits: an IPv6 address, optionally followed by
 * `/` and a prefix length, such as `2001:db8::1/64`; unlike a network, the address may have bits set beyond the
 * prefix length. A text that is not an IPv6 interface is reported as an error with the code
 * [IpViolationCodes.INVALID_INTERFACE]. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the interface, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv6Interface(property: KProperty<*>, text: String): Ipv6Interface? =
    when (val reading = readCidr(text, IpVersion.V6)) {
        is CidrReading.Read -> Ipv6Interface.assembled(Ipv6Address.fromBits(reading.bits), reading.prefixLength)

        is CidrReading.Refused -> refuse(
            property = property,
            message = "is not an IPv6 interface: it ${reading.reason}",
            code = IpViolationCodes.INVALID_INTERFACE,
        )
    }

/**
 * Reads [text] as an IPv4 range inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv4Range.parse] reads it, with the same limits: a single IPv4 address, or two joined by a
 * dash, such as `192.0.2.1-192.0.2.9`, whose first address is not after the last. A text that is not an IPv4 range
 * is reported as an error with the code [IpViolationCodes.INVALID_RANGE] and a message such as `is not an IPv4
 * range: it starts after it ends`. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the range, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv4Range(property: KProperty<*>, text: String): Ipv4Range? =
    when (val reading = readRange(text, IpVersion.V4)) {
        is RangeReading.Read -> Ipv4Range.assembled(
            first = Ipv4Address.fromBits(reading.first),
            last = Ipv4Address.fromBits(reading.last),
        )

        is RangeReading.Refused -> refuse(
            property = property,
            message = "is not an IPv4 range: it ${reading.reason}",
            code = IpViolationCodes.INVALID_RANGE,
        )
    }

/**
 * Reads [text] as an IPv6 range inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Ipv6Range.parse] reads it, with the same limits: a single IPv6 address, or two joined by a
 * dash, such as `2001:db8::1-2001:db8::9`, whose first address is not after the last. A text that is not an IPv6
 * range is reported as an error with the code [IpViolationCodes.INVALID_RANGE] and a message such as `is not an IPv6
 * range: it starts after it ends`. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the range, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.ipv6Range(property: KProperty<*>, text: String): Ipv6Range? =
    when (val reading = readRange(text, IpVersion.V6)) {
        is RangeReading.Read -> Ipv6Range.assembled(
            first = Ipv6Address.fromBits(reading.first),
            last = Ipv6Address.fromBits(reading.last),
        )

        is RangeReading.Refused -> refuse(
            property = property,
            message = "is not an IPv6 range: it ${reading.reason}",
            code = IpViolationCodes.INVALID_RANGE,
        )
    }
