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

package com.davils.konnect.internetprotocol.address

/**
 * Reads this text as an IPv4 or an IPv6 address, as [IpAddress.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address.
 * @throws IllegalArgumentException if this text is not an address; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpAddress(): IpAddress = IpAddress.parse(this)

/**
 * Reads this text as an IPv4 or an IPv6 address, as [IpAddress.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpAddressOrNull(): IpAddress? = IpAddress.parseOrNull(this)

/**
 * Reads this text as an IPv4 address, as [Ipv4Address.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address.
 * @throws IllegalArgumentException if this text is not an IPv4 address; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv4Address(): Ipv4Address = Ipv4Address.parse(this)

/**
 * Reads this text as an IPv4 address, as [Ipv4Address.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv4AddressOrNull(): Ipv4Address? = Ipv4Address.parseOrNull(this)

/**
 * Reads this text as an IPv6 address, as [Ipv6Address.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address.
 * @throws IllegalArgumentException if this text is not an IPv6 address; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv6Address(): Ipv6Address = Ipv6Address.parse(this)

/**
 * Reads this text as an IPv6 address, as [Ipv6Address.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the address, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv6AddressOrNull(): Ipv6Address? = Ipv6Address.parseOrNull(this)
