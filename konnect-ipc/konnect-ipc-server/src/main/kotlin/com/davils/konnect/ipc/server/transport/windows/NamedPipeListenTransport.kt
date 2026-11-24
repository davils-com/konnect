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

package com.davils.konnect.ipc.server.transport.windows

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.PipeSession
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.ipc.server.transport.ListenTransport
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary
import kotlinx.coroutines.CoroutineDispatcher

private const val UNAVAILABLE: String = "the native named pipe library or the identity of the current user is missing"
private const val UNREACHABLE: String = "the address cannot be reached through a Windows named pipe"

internal class NamedPipeListenTransport(
    private val library: NativeLibrary<NamedPipeApi>,
    private val dispatcher: CoroutineDispatcher,
    private val currentProcessId: Long,
) : ListenTransport {
    private val session: PipeSession? by lazy { PipeSession.open(library, currentProcessId) }

    override val currentUser: IpcUser? get() = session?.user

    override suspend fun listen(address: IpcAddress, access: IpcAccess, bufferSize: Int): ListenOutcome {
        val target = session?.targetOf(address) ?: return ListenOutcome.Refused(IpcBindResult.Unavailable(reason()))
        val opened = target.session
        val instances = PipeInstances(
            api = opened.api,
            path = target.path,
            securityDescriptor = PipeSecurity.descriptor(opened.user, access),
            bufferSize = bufferSize,
        )
        return PipeBinder(opened.api, opened.identity, dispatcher).bind(instances)
    }

    private fun reason(): String {
        if (session == null) return UNAVAILABLE
        return UNREACHABLE
    }
}
