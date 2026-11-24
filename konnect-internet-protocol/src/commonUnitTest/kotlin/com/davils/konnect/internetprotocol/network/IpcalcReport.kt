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

package com.davils.konnect.internetprotocol.network

import com.davils.konnect.internetprotocol.address.AddressCategory

internal data class IpcalcReport(
    val address: String,
    val binary: String,
    val netmask: String,
    val wildcard: String,
    val network: String,
    val broadcast: String?,
    val firstHost: String,
    val lastHost: String,
    val hosts: Long,
    val addressClass: String,
    val reverseDns: String,
    val category: AddressCategory,
) {
    companion object {
        fun describe(host: Ipv4Interface): IpcalcReport {
            val network = host.network
            return IpcalcReport(
                address = host.address.toString(),
                binary = host.address.toBinaryString(),
                netmask = "${host.netmask} = ${host.prefixLength}",
                wildcard = network.wildcard.toString(),
                network = network.toString(),
                broadcast = network.broadcast?.toString(),
                firstHost = network.firstHost.toString(),
                lastHost = network.lastHost.toString(),
                hosts = network.hostCount.toULongOrNull()?.toLong() ?: -1L,
                addressClass = host.address.addressClass.name,
                reverseDns = host.address.reverseDnsName,
                category = host.address.category,
            )
        }
    }
}
