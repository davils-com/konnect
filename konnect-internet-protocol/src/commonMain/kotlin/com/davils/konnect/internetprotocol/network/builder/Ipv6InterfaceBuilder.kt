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

package com.davils.konnect.internetprotocol.subnet.builder

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.subnet.Ipv6Interface
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder behind [ipv6Interface][com.davils.konnect.internetprotocol.subnet.ipv6Interface], which collects the
 * address and the prefix length of an [Ipv6Interface].
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one interface.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class Ipv6InterfaceBuilder internal constructor() : ValidatingBuilder<Ipv6Interface>() {
    override val subject: String get() = "interface"

    /**
     * The address of the host, which may have host bits set; [Ipv6Address.UNSPECIFIED], `::`, unless it is set.
     *
     * @since 1.0.0
     */
    public var address: Ipv6Address = Ipv6Address.UNSPECIFIED
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The length of the prefix, from `0` to `128`; `128` unless it is set.
     *
     * @since 1.0.0
     */
    public var prefixLength: Int = IpVersion.V6.bitLength
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Ipv6Interface = Ipv6Interface.assembled(address, prefixLength)
}
