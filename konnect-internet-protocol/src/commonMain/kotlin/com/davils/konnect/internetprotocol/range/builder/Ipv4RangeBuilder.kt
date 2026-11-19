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

package com.davils.konnect.internetprotocol.range.builder

import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.range.Ipv4Range
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder behind [ipv4Range][com.davils.konnect.internetprotocol.range.ipv4Range], which collects the first and the
 * last address of an [Ipv4Range].
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one range.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class Ipv4RangeBuilder internal constructor() : ValidatingBuilder<Ipv4Range>() {
    override val subject: String get() = "range"

    /**
     * The lowest address of the range; [Ipv4Address.UNSPECIFIED], `0.0.0.0`, unless it is set.
     *
     * @since 1.0.0
     */
    public var first: Ipv4Address = Ipv4Address.UNSPECIFIED
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The highest address of the range; [Ipv4Address.UNSPECIFIED], `0.0.0.0`, unless it is set.
     *
     * @since 1.0.0
     */
    public var last: Ipv4Address = Ipv4Address.UNSPECIFIED
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Ipv4Range = Ipv4Range.assembled(first, last)
}
