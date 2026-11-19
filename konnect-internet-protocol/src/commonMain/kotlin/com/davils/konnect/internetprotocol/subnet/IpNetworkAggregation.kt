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
import com.davils.konnect.internetprotocol.subnet.arithmetic.CidrBlock
import com.davils.konnect.internetprotocol.subnet.arithmetic.Interval
import com.davils.konnect.internetprotocol.subnet.arithmetic.cidrBlocks
import com.davils.konnect.internetprotocol.subnet.arithmetic.merged
import kotlin.jvm.JvmName

/**
 * Aggregates these networks into the fewest networks that hold exactly the same addresses.
 *
 * Networks that overlap or touch are merged, and networks that lie inside others or appear twice disappear:
 * `192.0.2.0/25` and `192.0.2.128/25` become `192.0.2.0/24`. Each merged block of addresses is then cut into the
 * fewest networks that cover it, so `192.0.2.0/24` and `192.0.3.0/25` stay two networks. The result never holds an
 * address that none of these networks holds. On the JVM, this function is named `aggregateIpv4`.
 *
 * @receiver the networks to aggregate, in any order.
 * @return the aggregated networks, in ascending order and without overlaps; empty if there are no networks.
 * @since 1.0.0
 */
@JvmName("aggregateIpv4")
public fun Iterable<Ipv4Network>.aggregate(): List<Ipv4Network> {
    val blocks = aggregatedBlocks(this, IpVersion.V4)
    return buildList { blocks.forEach { block -> add(Ipv4Network.fromBits(block.bits, block.prefixLength)) } }
}

/**
 * Aggregates these networks into the fewest networks that hold exactly the same addresses.
 *
 * Networks that overlap or touch are merged, and networks that lie inside others or appear twice disappear:
 * `2001:db8::/33` and `2001:db8:8000::/33` become `2001:db8::/32`. Each merged block of addresses is then cut into
 * the fewest networks that cover it, so `2001:db8::/32` and `2001:db9::/33` stay two networks. The result never holds
 * an address that none of these networks holds. On the JVM, this function is named `aggregateIpv6`.
 *
 * @receiver the networks to aggregate, in any order.
 * @return the aggregated networks, in ascending order and without overlaps; empty if there are no networks.
 * @since 1.0.0
 */
@JvmName("aggregateIpv6")
public fun Iterable<Ipv6Network>.aggregate(): List<Ipv6Network> {
    val blocks = aggregatedBlocks(this, IpVersion.V6)
    return buildList { blocks.forEach { block -> add(Ipv6Network.fromBits(block.bits, block.prefixLength)) } }
}

private fun aggregatedBlocks(networks: Iterable<IpNetwork>, version: IpVersion): List<CidrBlock> {
    val intervals = networks.map { network -> Interval(first = network.bits, last = network.lastBits) }
    return merged(intervals).flatMap { interval -> cidrBlocks(interval, version) }
}
