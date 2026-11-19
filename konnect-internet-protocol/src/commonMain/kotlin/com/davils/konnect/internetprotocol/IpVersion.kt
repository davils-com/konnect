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

private const val IPV4_BITS: Int = 32
private const val IPV6_BITS: Int = 128

/**
 * The two versions of the Internet Protocol that Konnect supports, which every address, network, interface and
 * range belongs to.
 *
 * Versions are ordered as declared, [V4] before [V6], and an address or a network of version 4 is ordered before
 * one of version 6.
 *
 * @property bitLength the number of bits in an address of this version: `32` for [V4] and `128` for [V6].
 * @since 1.0.0
 */
public enum class IpVersion(public val bitLength: Int) {
    /**
     * Internet Protocol version 4 of RFC 791, with addresses of 32 bits.
     *
     * @since 1.0.0
     */
    V4(IPV4_BITS),

    /**
     * Internet Protocol version 6 of RFC 8200, with addresses of 128 bits.
     *
     * @since 1.0.0
     */
    V6(IPV6_BITS),
}
