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
import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.subnet.Ipv4Network
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder behind [ipv4Network][com.davils.konnect.internetprotocol.subnet.ipv4Network], which collects the address
 * and the prefix length of an [Ipv4Network].
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one network.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class Ipv4NetworkBuilder internal constructor() : ValidatingBuilder<Ipv4Network>() {
    override val subject: String get() = "network"

    /**
     * The address of the network, which must have no host bits set at [prefixLength]; [Ipv4Address.UNSPECIFIED],
     * `0.0.0.0`, unless it is set. An address with host bits set is refused when the network is built, never masked.
     *
     * @since 1.0.0
     */
    public var address: Ipv4Address = Ipv4Address.UNSPECIFIED
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The length of the prefix, from `0` to `32`; `32` unless it is set.
     *
     * @since 1.0.0
     */
    public var prefixLength: Int = IpVersion.V4.bitLength
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Ipv4Network = Ipv4Network.assembled(address, prefixLength)
}
