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

package com.davils.konnect.core.port

/**
 * Builds a [PortRange] in a DSL, such as `portRange { first = Port(8000); last = Port(8080) }`.
 *
 * A bound that is not set stays at [Port.MIN] or [Port.MAX], so `portRange {}` is [PortRange.ALL].
 *
 * @param block the block that sets the bounds.
 * @return the range.
 * @throws com.davils.kore.core.validation.report.ValidationException if the last port is below the first; the
 * report holds one error at `last` with the code [PortViolationCodes.INVALID_PORT_RANGE].
 * @since 1.0.0
 */
public fun portRange(block: PortRangeBuilder.() -> Unit): PortRange {
    val builder = PortRangeBuilder()
    builder.block()
    return builder.build()
}

/**
 * Merges these ranges into the fewest ranges that hold the same ports.
 *
 * Ranges that overlap or touch, such as `8000-8080` and `8081-8090`, become one range. The result is sorted by the
 * first port and holds no two ranges that overlap or touch.
 *
 * @receiver the ranges to merge, in any order.
 * @return the merged ranges, in ascending order; empty if there are no ranges.
 * @since 1.0.0
 */
public fun Iterable<PortRange>.merge(): List<PortRange> {
    val sorted = sortedBy(PortRange::first)
    return buildList {
        sorted.forEach { range ->
            val previous = lastOrNull()
            if (previous == null || range.first.value > previous.last.value + 1) {
                add(range)
                return@forEach
            }
            val last = maxOf(a = previous.last, b = range.last)
            set(lastIndex, PortRange.assembled(first = previous.first, last = last))
        }
    }
}
