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

package com.davils.konnect.internetprotocol.address.registry

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.bits.Bits128
import com.davils.konnect.internetprotocol.address.bits.prefixMask

internal class RegistryEntry(
    private val version: IpVersion,
    private val bits: Bits128,
    val prefixLength: Int,
    val purpose: SpecialPurpose,
) {
    fun covers(address: IpAddress): Boolean {
        if (address.version != version) return false
        return address.bits intersect prefixMask(version, prefixLength) == bits
    }
}
