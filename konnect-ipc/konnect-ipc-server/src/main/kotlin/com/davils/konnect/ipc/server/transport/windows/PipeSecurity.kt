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

package com.davils.konnect.ipc.server.transport.windows

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser

private const val PROTECTED_DACL: String = "D:P"
private const val FULL_ACCESS: String = "GA"
private const val READ_WRITE: String = "GRGW"
private const val MEDIUM_LABEL: String = "S:(ML;;NW;;;ME)"

internal object PipeSecurity {
    fun descriptor(server: IpcUser.WindowsSid, access: IpcAccess): String {
        val sids = access.users.filterIsInstance<IpcUser.WindowsSid>()
        val clients = sids.filter { sid -> sid != server }
        val entries = listOf(entry(FULL_ACCESS, server)) + clients.map { client -> entry(READ_WRITE, client) }
        return PROTECTED_DACL + entries.joinToString(separator = "") + labelOf(access)
    }

    private fun entry(rights: String, sid: IpcUser.WindowsSid): String = "(A;;$rights;;;${sid.value})"

    private fun labelOf(access: IpcAccess): String {
        if (access.isCurrentUserOnly) return ""
        return MEDIUM_LABEL
    }
}
