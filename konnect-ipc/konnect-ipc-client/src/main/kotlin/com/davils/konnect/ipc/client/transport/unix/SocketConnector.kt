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

package com.davils.konnect.ipc.client.transport.unix

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.core.transport.toSystemError
import com.davils.konnect.ipc.core.transport.unix.UnixChannel
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.net.ConnectException
import java.nio.file.Path

internal class SocketConnector(private val api: UnixSocketApi, private val dispatcher: CoroutineDispatcher) {
    fun connect(path: Path): ConnectOutcome {
        val refusal = refusalBefore(path)
        if (refusal != null) return ConnectOutcome.Refused(refusal)
        return opened(path)
    }

    private fun refusalBefore(path: Path): IpcConnectResult? {
        if (!api.exists(path)) return IpcConnectResult.NotFound
        return IpcConnectResult.AccessDenied.takeUnless { api.isWritable(path) }
    }

    private fun opened(path: Path): ConnectOutcome = try {
        ConnectOutcome.Connected(UnixChannel(api.connect(path), dispatcher))
    } catch (expectedRefusal: ConnectException) {
        ConnectOutcome.Refused(IpcConnectResult.NotFound)
    } catch (expectedFailure: IOException) {
        ConnectOutcome.Refused(IpcConnectResult.Failed(expectedFailure.toSystemError()))
    }
}
