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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

internal data class Router(
    val loopback: String,
    val management: String,
    val managementV6: String,
    val lan: String,
    val lanV6: String,
    val uplink: String,
    val uplinkV6: String,
    val pool: String,
    val poolV6: String,
) : Validatable {
    private val readings: MutableList<Any?> = mutableListOf()

    val values: List<Any?> get() = readings.toList()

    override fun ValidationScope.validate() {
        readings.clear()
        readings += ipAddress(::loopback, loopback)
        readings += ipv4Address(::management, management)
        readings += ipv6Address(::managementV6, managementV6)
        readings += ipv4Network(::lan, lan)
        readings += ipv6Network(::lanV6, lanV6)
        readings += ipv4Interface(::uplink, uplink)
        readings += ipv6Interface(::uplinkV6, uplinkV6)
        readings += ipv4Range(::pool, pool)
        readings += ipv6Range(::poolV6, poolV6)
    }
}
