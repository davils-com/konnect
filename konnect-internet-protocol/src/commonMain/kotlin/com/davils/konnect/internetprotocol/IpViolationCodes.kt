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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations that the validations of IP addresses, networks, interfaces and ranges report.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object IpViolationCodes {
    /**
     * The code of a text that is not an IP address, reported by [ipAddress], [ipv4Address] and [ipv6Address]; its
     * value is `konnect.ip.invalid-address`.
     *
     * @since 1.0.0
     */
    public val INVALID_ADDRESS: ViolationCode = ViolationCode("konnect.ip.invalid-address")

    /**
     * The code of a text that is not an IP network, including a network text with host bits set, reported by
     * [ipv4Network] and [ipv6Network]; its value is `konnect.ip.invalid-network`.
     *
     * @since 1.0.0
     */
    public val INVALID_NETWORK: ViolationCode = ViolationCode("konnect.ip.invalid-network")

    /**
     * The code of a text that is not an IP interface, reported by [ipv4Interface] and [ipv6Interface]; its value is
     * `konnect.ip.invalid-interface`.
     *
     * @since 1.0.0
     */
    public val INVALID_INTERFACE: ViolationCode = ViolationCode("konnect.ip.invalid-interface")

    /**
     * The code of a prefix length outside `0..32` for IPv4 or `0..128` for IPv6, reported at the prefix length by
     * the validation of a [network][com.davils.konnect.internetprotocol.network.IpNetwork] and of an
     * [interface][com.davils.konnect.internetprotocol.network.IpInterface]; its value is `konnect.ip.invalid-prefix`.
     *
     * @since 1.0.0
     */
    public val INVALID_PREFIX: ViolationCode = ViolationCode("konnect.ip.invalid-prefix")

    /**
     * The code of a network whose address has bits set beyond its prefix length, reported at the address by the
     * validation of a [network][com.davils.konnect.internetprotocol.network.IpNetwork]; its value is
     * `konnect.ip.host-bits-set`.
     *
     * @since 1.0.0
     */
    public val HOST_BITS_SET: ViolationCode = ViolationCode("konnect.ip.host-bits-set")

    /**
     * The code of a text that is not an IP range and of a range whose last address is before its first, reported by
     * [ipv4Range], [ipv6Range] and the validation of a [range][com.davils.konnect.internetprotocol.range.IpRange];
     * its value is `konnect.ip.invalid-range`.
     *
     * @since 1.0.0
     */
    public val INVALID_RANGE: ViolationCode = ViolationCode("konnect.ip.invalid-range")
}
