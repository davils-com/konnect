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

package com.davils.konnect.internetprotocol.address

/**
 * The historic address classes of RFC 791 and RFC 1112, which an [Ipv4Address] belongs to by the leading bits of its
 * first octet.
 *
 * Classful addressing was replaced by CIDR in RFC 4632; the class of an address says nothing about the network it
 * belongs to today and is kept for display and for legacy configurations only.
 *
 * @since 1.0.0
 */
public enum class Ipv4Class {
    /**
     * Class A, a first octet from `0` to `127`, whose leading bit is `0`.
     *
     * @since 1.0.0
     */
    A,

    /**
     * Class B, a first octet from `128` to `191`, whose leading bits are `10`.
     *
     * @since 1.0.0
     */
    B,

    /**
     * Class C, a first octet from `192` to `223`, whose leading bits are `110`.
     *
     * @since 1.0.0
     */
    C,

    /**
     * Class D, a first octet from `224` to `239`, whose leading bits are `1110`: the multicast addresses.
     *
     * @since 1.0.0
     */
    D,

    /**
     * Class E, a first octet from `240` to `255`, whose leading bits are `1111`: the reserved addresses, together
     * with the limited broadcast address.
     *
     * @since 1.0.0
     */
    E,
    ;

    internal companion object {
        fun forFirstOctet(firstOctet: Int): Ipv4Class {
            val inverted = firstOctet.toUByte().inv()
            val leadingOnes = inverted.countLeadingZeroBits()
            return entries[leadingOnes.coerceAtMost(E.ordinal)]
        }
    }
}
