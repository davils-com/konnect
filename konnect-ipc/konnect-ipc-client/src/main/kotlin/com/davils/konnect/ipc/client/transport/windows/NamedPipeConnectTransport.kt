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

package com.davils.konnect.ipc.client.transport.windows

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.client.transport.ConnectTransport
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.PipeSession
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val UNAVAILABLE: String = "the native named pipe library or the identity of the current user is missing"

internal class NamedPipeConnectTransport(
    private val library: NativeLibrary<NamedPipeApi>,
    private val dispatcher: CoroutineDispatcher,
    private val currentProcessId: Long,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : ConnectTransport {
    private val session: PipeSession? by lazy { PipeSession.open(library, currentProcessId) }

    override val currentUser: IpcUser? get() = session?.user

    override suspend fun connect(address: IpcAddress, timeout: Duration, bufferSize: Int): ConnectOutcome {
        val target = session?.targetOf(address)
            ?: return ConnectOutcome.Refused(IpcConnectResult.Unavailable(reasonFor(address)))
        val opened = target.session
        val connector = PipeConnector(opened.api, opened.identity, dispatcher)
        val deadline = timeSource.markNow() + timeout
        return connector.connect(target.path, deadline = deadline, bufferSize = bufferSize)
    }

    private fun reasonFor(address: IpcAddress): String {
        if (session == null) return UNAVAILABLE
        return "$address cannot be reached through a Windows named pipe"
    }
}
