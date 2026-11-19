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

/**
 * Reads this text as an IPv4 network, as [Ipv4Network.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 network.
 * @throws IllegalArgumentException if this text is not an IPv4 network; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv4Network(): Ipv4Network = Ipv4Network.parse(this)

/**
 * Reads this text as an IPv4 network, as [Ipv4Network.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 network, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv4NetworkOrNull(): Ipv4Network? = Ipv4Network.parseOrNull(this)

/**
 * Reads this text as an IPv6 network, as [Ipv6Network.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 network.
 * @throws IllegalArgumentException if this text is not an IPv6 network; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv6Network(): Ipv6Network = Ipv6Network.parse(this)

/**
 * Reads this text as an IPv6 network, as [Ipv6Network.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 network, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv6NetworkOrNull(): Ipv6Network? = Ipv6Network.parseOrNull(this)

/**
 * Reads this text as an IPv4 interface, as [Ipv4Interface.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 interface.
 * @throws IllegalArgumentException if this text is not an IPv4 interface; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv4Interface(): Ipv4Interface = Ipv4Interface.parse(this)

/**
 * Reads this text as an IPv4 interface, as [Ipv4Interface.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 interface, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv4InterfaceOrNull(): Ipv4Interface? = Ipv4Interface.parseOrNull(this)

/**
 * Reads this text as an IPv6 interface, as [Ipv6Interface.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 interface.
 * @throws IllegalArgumentException if this text is not an IPv6 interface; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv6Interface(): Ipv6Interface = Ipv6Interface.parse(this)

/**
 * Reads this text as an IPv6 interface, as [Ipv6Interface.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 interface, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv6InterfaceOrNull(): Ipv6Interface? = Ipv6Interface.parseOrNull(this)
