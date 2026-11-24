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
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.environment.IpcEnvironment
import com.davils.konnect.ipc.core.transport.unix.SocketLocations
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.ipc.server.transport.ListenTransport
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runInterruptible

private const val UNREACHABLE: String = "the address cannot be reached through a Unix domain socket"

internal class UnixListenTransport(
    private val api: UnixSocketApi,
    private val environment: IpcEnvironment,
    isMacOs: Boolean,
    private val dispatcher: CoroutineDispatcher,
) : ListenTransport {
    private val locations = SocketLocations(environment, isMacOs)

    override val currentUser: IpcUser?
        get() {
            val name = environment.userName
            if (!IpcUser.UnixUser.isValid(name)) return null
            return IpcUser.UnixUser(name)
        }

    override suspend fun listen(address: IpcAddress, access: IpcAccess, bufferSize: Int): ListenOutcome {
        val location = locations.of(address) ?: return ListenOutcome.Refused(IpcBindResult.Unavailable(UNREACHABLE))
        val binder = SocketBinder(api, environment.userName, dispatcher)
        return runInterruptible(dispatcher) { binder.bind(location, access) }
    }
}
