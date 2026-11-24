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

/**
 * The upper bounds of the internet protocol module on everything it reads from outside the program.
 *
 * They narrow the limits every Konnect module shares, which live in
 * [com.davils.konnect.core.Limits]: an address text is first held to the general text limit and then to the tighter
 * bound of its version. A text above a limit is refused like any other malformed text; it is never truncated.
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one is a breaking change.
 * Each limit is a compile-time constant, so code that reads one keeps the value it was compiled against until it is
 * compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The longest text, in UTF-16 code units, that is read as an IPv6 address.
     *
     * It is the length of the longest canonical form, eight groups of which the last two are written as an embedded
     * IPv4 address, such as `ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255`. A longer address text is refused before
     * its groups are read.
     *
     * @since 1.0.0
     */
    public const val MAX_IPV6_ADDRESS_LENGTH: Int = 45
}
