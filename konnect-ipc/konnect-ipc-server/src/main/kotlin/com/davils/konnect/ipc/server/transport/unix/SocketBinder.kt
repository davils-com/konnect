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

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.transport.toSystemError
import com.davils.konnect.ipc.core.transport.unix.SocketLocation
import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.random.SecureEntropy
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.net.BindException
import java.net.ConnectException
import java.nio.file.Path

private const val HEX_RADIX: Int = 16
private const val TOKEN_LENGTH: Int = 8
private const val TAKEN: String = "the address was taken while the socket was staged"

internal class SocketBinder(
    private val api: UnixSocketApi,
    private val userName: String,
    private val dispatcher: CoroutineDispatcher,
    private val tokens: () -> String = ::randomToken,
) {
    fun bind(location: SocketLocation, access: IpcAccess): ListenOutcome = try {
        bound(location, access)
    } catch (expectedInUse: BindException) {
        ListenOutcome.Refused(IpcBindResult.AddressInUse)
    } catch (expectedFailure: IOException) {
        ListenOutcome.Refused(IpcBindResult.Failed(expectedFailure.toSystemError()))
    }

    private fun bound(location: SocketLocation, access: IpcAccess): ListenOutcome {
        val refusal = prepared(location.managedDirectory, access) ?: claimed(location.path)
        if (refusal != null) return ListenOutcome.Refused(refusal)
        return staged(location.path, access)
    }

    private fun staged(path: Path, access: IpcAccess): ListenOutcome {
        val staging = SocketStaging(api, path, tokens())
        val refusal = staging.prepared(userName)
        if (refusal != null) return ListenOutcome.Refused(refusal)
        try {
            return ListenOutcome.Listening(moved(path, staging, access))
        } finally {
            staging.remove()
        }
    }

    private fun prepared(directory: Path?, access: IpcAccess): IpcBindResult? {
        if (directory == null) return null
        return try {
            api.createDirectories(directory)
            owned(directory, access)
        } catch (expectedDenied: IOException) {
            IpcBindResult.AccessDenied
        }
    }

    private fun owned(directory: Path, access: IpcAccess): IpcBindResult? {
        if (api.owner(directory) != userName) return IpcBindResult.AccessDenied
        api.restrict(directory, SocketPermissions.directory(access))
        return null
    }

    private fun claimed(path: Path): IpcBindResult? {
        val isTaken = api.exists(path) && isAlive(path)
        if (isTaken) return IpcBindResult.AddressInUse
        api.delete(path)
        return null
    }

    private fun isAlive(path: Path): Boolean = try {
        api.connect(path).close()
        true
    } catch (expectedStale: ConnectException) {
        false
    } catch (expectedUnknown: IOException) {
        true
    }

    private fun moved(path: Path, staging: SocketStaging, access: IpcAccess): UnixListener {
        val server = api.bind(staging.socket)
        var isMoved = false
        try {
            api.restrict(staging.socket, SocketPermissions.socket(access))
            if (api.exists(path)) throw BindException(TAKEN)
            api.move(source = staging.socket, target = path)
            isMoved = true
        } finally {
            closeUnless(isMoved, server)
        }
        return UnixListener(
            server = server,
            path = path,
            staging = staging.directory,
            api = api,
            dispatcher = dispatcher,
        )
    }

    private fun closeUnless(isMoved: Boolean, server: UnixServerSocket) {
        if (isMoved) return
        try {
            server.close()
        } catch (expectedFailure: IOException) {
            return
        }
    }
}

private fun randomToken(): String {
    val value = SecureEntropy.nextLong().toInt()
    val text = value.toUInt().toString(radix = HEX_RADIX)
    return text.padStart(length = TOKEN_LENGTH, padChar = '0')
}
