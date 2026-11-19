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

private const val REGISTERED_START: Int = 1024
private const val DYNAMIC_START: Int = 49_152

/**
 * The three ranges of ports that RFC 6335 defines, which a [Port] belongs to by its number.
 *
 * @since 1.0.0
 */
public enum class PortCategory(internal val start: Int) {
    /**
     * The system or well-known ports, `0` to `1023`, which IANA assigns and which many systems open only to a
     * privileged process.
     *
     * @since 1.0.0
     */
    WellKnown(0),

    /**
     * The user or registered ports, `1024` to `49151`, which IANA assigns on request.
     *
     * @since 1.0.0
     */
    Registered(REGISTERED_START),

    /**
     * The dynamic or private ports, `49152` to `65535`, which IANA never assigns and which systems hand out as
     * ephemeral ports.
     *
     * @since 1.0.0
     */
    Dynamic(DYNAMIC_START),
    ;

    internal companion object {
        fun forPort(value: Int): PortCategory = entries.last { category -> value >= category.start }
    }
}
