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

/**
 * Reads this text as an IPv4 netmask, as [Ipv4Netmask.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 netmask.
 * @throws IllegalArgumentException if this text is not an IPv4 netmask; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv4Netmask(): Ipv4Netmask = Ipv4Netmask.parse(this)

/**
 * Reads this text as an IPv4 netmask, as [Ipv4Netmask.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 netmask, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv4NetmaskOrNull(): Ipv4Netmask? = Ipv4Netmask.parseOrNull(this)

/**
 * Reads this text as an IPv6 netmask, as [Ipv6Netmask.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 netmask.
 * @throws IllegalArgumentException if this text is not an IPv6 netmask; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv6Netmask(): Ipv6Netmask = Ipv6Netmask.parse(this)

/**
 * Reads this text as an IPv6 netmask, as [Ipv6Netmask.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 netmask, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv6NetmaskOrNull(): Ipv6Netmask? = Ipv6Netmask.parseOrNull(this)
