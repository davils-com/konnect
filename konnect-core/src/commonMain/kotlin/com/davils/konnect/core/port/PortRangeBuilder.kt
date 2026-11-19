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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder behind [portRange], which collects the first and the last port of a [PortRange].
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one range. Once
 * it was evaluated by `build()` or `validate()`, setting a bound throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PortRangeBuilder internal constructor() : ValidatingBuilder<PortRange>() {
    override val subject: String get() = "port range"

    /**
     * The lowest port of the range; [Port.MIN] unless it is set.
     *
     * @since 1.0.0
     */
    public var first: Port = Port.MIN
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The highest port of the range; [Port.MAX] unless it is set.
     *
     * @since 1.0.0
     */
    public var last: Port = Port.MAX
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): PortRange = PortRange.assembled(first = first, last = last)
}
