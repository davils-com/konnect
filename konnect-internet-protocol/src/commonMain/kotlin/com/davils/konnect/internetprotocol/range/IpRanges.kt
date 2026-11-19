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

import com.davils.konnect.internetprotocol.range.builder.Ipv4RangeBuilder
import com.davils.konnect.internetprotocol.range.builder.Ipv6RangeBuilder

/**
 * Builds an [Ipv4Range] in a DSL, such as
 * `ipv4Range { first = "192.0.2.10".toIpv4Address(); last = "192.0.2.20".toIpv4Address() }`.
 *
 * A bound that is not set stays at
 * [Ipv4Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv4Address.UNSPECIFIED], `0.0.0.0`, so
 * `ipv4Range {}` is the range of that single address, and a block that sets only a first address above it fails.
 *
 * @param block the block that sets the bounds.
 * @return the range.
 * @throws com.davils.kore.core.validation.report.ValidationException if the last address is before the first; the
 * report holds one error at `last` with the code
 * [IpViolationCodes.INVALID_RANGE][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_RANGE].
 * @since 1.0.0
 */
public fun ipv4Range(block: Ipv4RangeBuilder.() -> Unit): Ipv4Range {
    val builder = Ipv4RangeBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builds an [Ipv6Range] in a DSL, such as
 * `ipv6Range { first = "2001:db8::10".toIpv6Address(); last = "2001:db8::20".toIpv6Address() }`.
 *
 * A bound that is not set stays at
 * [Ipv6Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv6Address.UNSPECIFIED], `::`, so
 * `ipv6Range {}` is the range of that single address, and a block that sets only a first address above it fails.
 *
 * @param block the block that sets the bounds.
 * @return the range.
 * @throws com.davils.kore.core.validation.report.ValidationException if the last address is before the first; the
 * report holds one error at `last` with the code
 * [IpViolationCodes.INVALID_RANGE][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_RANGE].
 * @since 1.0.0
 */
public fun ipv6Range(block: Ipv6RangeBuilder.() -> Unit): Ipv6Range {
    val builder = Ipv6RangeBuilder()
    builder.block()
    return builder.build()
}
