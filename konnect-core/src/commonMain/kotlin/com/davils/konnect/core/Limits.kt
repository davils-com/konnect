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

package com.davils.konnect.core

/**
 * The upper bounds Konnect enforces on everything it reads from outside the program.
 *
 * Every parser of Konnect checks its input against these limits before it does any work that grows with the size of
 * that input, so a hostile text can neither exhaust the memory of a process nor keep it busy. A text above a limit
 * is refused like any other malformed text, with an exception, a violation or a `null` result as the entry point
 * documents; it is never truncated.
 *
 * The limits are generous for every legitimate text and are part of the public contract: raising one is a
 * compatible change, lowering one is a breaking change. Each limit is a compile-time constant, so code that reads one
 * keeps the value it was compiled against until it is compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The longest text, in UTF-16 code units, that any parser of Konnect reads.
     *
     * It covers the longest canonical form of every value Konnect reads, such as a range of two IPv6 addresses with
     * an embedded IPv4 address each, with room for the spaces a range may hold around its dash. A longer text is
     * refused before it is split or scanned.
     *
     * @since 1.0.0
     */
    public const val MAX_TEXT_LENGTH: Int = 128

    /**
     * The most subnets that one call splits a network into at once.
     *
     * A split builds its whole list before it returns, so a larger count is refused before any subnet is created. A
     * caller that needs more subnets walks the lazy sequence of all subnets of a prefix length instead.
     *
     * @since 1.0.0
     */
    public const val MAX_SPLIT_COUNT: Int = 65_536
}
