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

import com.davils.konnect.internetprotocol.subnet.builder.Ipv4InterfaceBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv4NetworkBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv6InterfaceBuilder
import com.davils.konnect.internetprotocol.subnet.builder.Ipv6NetworkBuilder

/**
 * Builds an [Ipv4Network] in a DSL, such as
 * `ipv4Network { address = Ipv4Address.parse("192.0.2.0"); prefixLength = 24 }`.
 *
 * An address that is not set stays at
 * [Ipv4Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv4Address.UNSPECIFIED], `0.0.0.0`, and a
 * prefix length that is not set stays at `32`, so `ipv4Network {}` is `0.0.0.0/32`. An address with host bits set is
 * refused, never masked.
 *
 * @param block the block that sets the address and the prefix length.
 * @return the network.
 * @throws com.davils.kore.core.validation.report.ValidationException if the prefix length is outside `0..32`, with one
 * error at `prefixLength` with the code
 * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX], or if the
 * address has host bits set, with one error at `address` with the code
 * [IpViolationCodes.HOST_BITS_SET][com.davils.konnect.internetprotocol.IpViolationCodes.HOST_BITS_SET].
 * @since 1.0.0
 */
public fun ipv4Network(block: Ipv4NetworkBuilder.() -> Unit): Ipv4Network {
    val builder = Ipv4NetworkBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builds an [Ipv6Network] in a DSL, such as
 * `ipv6Network { address = Ipv6Address.parse("2001:db8::"); prefixLength = 32 }`.
 *
 * An address that is not set stays at
 * [Ipv6Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv6Address.UNSPECIFIED], `::`, and a prefix
 * length that is not set stays at `128`, so `ipv6Network {}` is `::/128`. An address with host bits set is refused,
 * never masked.
 *
 * @param block the block that sets the address and the prefix length.
 * @return the network.
 * @throws com.davils.kore.core.validation.report.ValidationException if the prefix length is outside `0..128`, with one
 * error at `prefixLength` with the code
 * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX], or if the
 * address has host bits set, with one error at `address` with the code
 * [IpViolationCodes.HOST_BITS_SET][com.davils.konnect.internetprotocol.IpViolationCodes.HOST_BITS_SET].
 * @since 1.0.0
 */
public fun ipv6Network(block: Ipv6NetworkBuilder.() -> Unit): Ipv6Network {
    val builder = Ipv6NetworkBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builds an [Ipv4Interface] in a DSL, such as
 * `ipv4Interface { address = Ipv4Address.parse("192.0.2.10"); prefixLength = 24 }`.
 *
 * An address that is not set stays at
 * [Ipv4Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv4Address.UNSPECIFIED], `0.0.0.0`, and a
 * prefix length that is not set stays at `32`, so `ipv4Interface {}` is `0.0.0.0/32`. The address may have host bits
 * set.
 *
 * @param block the block that sets the address and the prefix length.
 * @return the interface.
 * @throws com.davils.kore.core.validation.report.ValidationException if the prefix length is outside `0..32`; the
 * report holds one error at `prefixLength` with the code
 * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX].
 * @since 1.0.0
 */
public fun ipv4Interface(block: Ipv4InterfaceBuilder.() -> Unit): Ipv4Interface {
    val builder = Ipv4InterfaceBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builds an [Ipv6Interface] in a DSL, such as
 * `ipv6Interface { address = Ipv6Address.parse("2001:db8::10"); prefixLength = 64 }`.
 *
 * An address that is not set stays at
 * [Ipv6Address.UNSPECIFIED][com.davils.konnect.internetprotocol.address.Ipv6Address.UNSPECIFIED], `::`, and a prefix
 * length that is not set stays at `128`, so `ipv6Interface {}` is `::/128`. The address may have host bits set.
 *
 * @param block the block that sets the address and the prefix length.
 * @return the interface.
 * @throws com.davils.kore.core.validation.report.ValidationException if the prefix length is outside `0..128`; the
 * report holds one error at `prefixLength` with the code
 * [IpViolationCodes.INVALID_PREFIX][com.davils.konnect.internetprotocol.IpViolationCodes.INVALID_PREFIX].
 * @since 1.0.0
 */
public fun ipv6Interface(block: Ipv6InterfaceBuilder.() -> Unit): Ipv6Interface {
    val builder = Ipv6InterfaceBuilder()
    builder.block()
    return builder.build()
}
