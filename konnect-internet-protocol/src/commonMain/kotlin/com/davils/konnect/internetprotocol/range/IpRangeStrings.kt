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

/**
 * Reads this text as an IPv4 range, as [Ipv4Range.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 range.
 * @throws IllegalArgumentException if this text is not an IPv4 range; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv4Range(): Ipv4Range = Ipv4Range.parse(this)

/**
 * Reads this text as an IPv4 range, as [Ipv4Range.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv4 range, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv4RangeOrNull(): Ipv4Range? = Ipv4Range.parseOrNull(this)

/**
 * Reads this text as an IPv6 range, as [Ipv6Range.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 range.
 * @throws IllegalArgumentException if this text is not an IPv6 range; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toIpv6Range(): Ipv6Range = Ipv6Range.parse(this)

/**
 * Reads this text as an IPv6 range, as [Ipv6Range.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the IPv6 range, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toIpv6RangeOrNull(): Ipv6Range? = Ipv6Range.parseOrNull(this)
