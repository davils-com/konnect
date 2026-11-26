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

import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.network.IpNetwork
import com.davils.kore.core.arguments.conversion.ConversionException
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters

/**
 * Converters of Kore for IP addresses and networks, so that a command line parameter of Kore, an environment read of
 * Knot or a property of Kontext reads them typed, such as `property("proxy.allowed", AddressConverters.ipNetwork)`.
 *
 * Every converter reads exactly the text [IpAddress.parse] and [IpNetwork.parse] read, IPv4 and IPv6 alike, and refuses
 * everything else, a zone identifier and a network with host bits set included, with a [ConversionException] of
 * constant text that never repeats the input.
 *
 * @since 1.1.0
 */
public object AddressConverters {
    /**
     * Reads an [IpAddress], such as `192.0.2.1` or `2001:db8::1`; its type name is `ip address`.
     *
     * @since 1.1.0
     */
    public val ipAddress: Converter<IpAddress> = Converters.custom(typeName = "ip address") { raw ->
        IpAddress.parseOrNull(raw) ?: throw ConversionException("is no IP address")
    }

    /**
     * Reads an [IpNetwork], such as `192.0.2.0/24` or `2001:db8::/32`; its type name is `ip network`.
     *
     * @since 1.1.0
     */
    public val ipNetwork: Converter<IpNetwork> = Converters.custom(typeName = "ip network") { raw ->
        IpNetwork.parseOrNull(raw) ?: throw ConversionException("is no IP network")
    }
}
