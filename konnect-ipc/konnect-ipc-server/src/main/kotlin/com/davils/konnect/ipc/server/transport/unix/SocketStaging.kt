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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.ipc.server.transport.unix

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException
import java.nio.file.Path

private const val PREFIX: String = ".konnect-"
private const val SOCKET: String = "s"
private const val NO_ROOM: String = "the path of the socket leaves no room for its private staging directory"

internal class SocketStaging(private val api: UnixSocketApi, path: Path, token: String) {
    val directory: Path = path.resolveSibling(PREFIX + token)

    val socket: Path = directory.resolve(SOCKET)

    fun prepared(userName: String): IpcBindResult? {
        val refusal = blocked()
        if (refusal != null) return refusal
        return created(userName)
    }

    fun remove() {
        quietly { api.delete(socket) }
        quietly { api.delete(directory) }
    }

    private fun blocked(): IpcBindResult? {
        if (!IpcAddress.UnixSocket.isValid(socket)) return IpcBindResult.Unavailable(NO_ROOM)
        return IpcBindResult.AccessDenied.takeIf { api.exists(directory) }
    }

    private fun created(userName: String): IpcBindResult? {
        api.createDirectories(directory)
        if (api.owner(directory) != userName) return IpcBindResult.AccessDenied
        api.restrict(directory, SocketPermissions.staging)
        return null
    }

    private inline fun quietly(action: () -> Unit) {
        try {
            action()
        } catch (expectedFailure: IOException) {
            return
        }
    }
}
