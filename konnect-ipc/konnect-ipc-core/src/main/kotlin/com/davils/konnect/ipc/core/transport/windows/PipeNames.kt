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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcScope
import com.davils.kore.core.annotation.DavilsInternal

private const val PREFIX: String = """\\.\pipe\"""
private const val NAMESPACE: String = "com.davils.konnect"
private const val SEPARATOR: String = """\"""

/**
 * Finds the path of the pipe of an address.
 *
 * A named address of the user lives at `\\.\pipe\com.davils.konnect\<SID>\<name>`, a named address of the system
 * at `\\.\pipe\com.davils.konnect\<name>` and a Windows pipe at `\\.\pipe\<name>`.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object PipeNames {
    /**
     * Finds the path of the pipe of [address] for [user].
     *
     * @param address the address.
     * @param user the user the process runs as.
     * @return the path, or `null` for a Unix socket.
     * @since 1.0.0
     */
    public fun of(address: IpcAddress, user: IpcUser.WindowsSid): String? = when (address) {
        is IpcAddress.Named -> PREFIX + named(address, user)
        is IpcAddress.WindowsPipe -> PREFIX + address.name
        is IpcAddress.UnixSocket -> null
    }

    private fun named(address: IpcAddress.Named, user: IpcUser.WindowsSid): String {
        val segments = when (address.scope) {
            IpcScope.User -> listOf(NAMESPACE, user.value, address.name.value)
            IpcScope.System -> listOf(NAMESPACE, address.name.value)
        }
        return segments.joinToString(SEPARATOR)
    }
}
