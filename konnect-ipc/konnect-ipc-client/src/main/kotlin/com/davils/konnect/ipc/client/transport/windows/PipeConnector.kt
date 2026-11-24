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
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.NamedPipeChannel
import com.davils.konnect.ipc.core.transport.windows.PipeHandle
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds

internal class PipeConnector(
    private val api: NamedPipeApi,
    private val identity: PipeIdentity,
    private val dispatcher: CoroutineDispatcher,
) {
    private val openRefusals: Map<Long, IpcConnectResult> = mapOf(
        Win32Error.FILE_NOT_FOUND to IpcConnectResult.NotFound,
        Win32Error.ACCESS_DENIED to IpcConnectResult.AccessDenied,
    )

    private val waitRefusals: Map<Long, IpcConnectResult> = mapOf(
        Win32Error.FILE_NOT_FOUND to IpcConnectResult.NotFound,
        Win32Error.SEM_TIMEOUT to IpcConnectResult.TimedOut,
    )

    tailrec suspend fun connect(path: String, deadline: ComparableTimeMark, bufferSize: Int): ConnectOutcome {
        val step = attempt(path, deadline, bufferSize)
        if (step != null) return step
        return connect(path, deadline, bufferSize)
    }

    private suspend fun attempt(path: String, deadline: ComparableTimeMark, bufferSize: Int): ConnectOutcome? {
        val opened = withContext(dispatcher) { api.open(path) }
        if (opened.isSuccess) return ConnectOutcome.Connected(channelOf(opened.value, bufferSize))
        return busyOrRefused(opened.errorCode, path, deadline)
    }

    private suspend fun busyOrRefused(code: Long, path: String, deadline: ComparableTimeMark): ConnectOutcome? {
        if (code != Win32Error.PIPE_BUSY) return ConnectOutcome.Refused(refusalOf(openRefusals, code))
        return waited(path, deadline)
    }

    private suspend fun waited(path: String, deadline: ComparableTimeMark): ConnectOutcome? {
        val remaining = -deadline.elapsedNow()
        if (remaining < 1.milliseconds) return ConnectOutcome.Refused(IpcConnectResult.TimedOut)
        val millis = remaining.inWholeMilliseconds.coerceAtMost(Int.MAX_VALUE.toLong())
        val result = withContext(dispatcher) { api.waitAvailable(path, millis.toInt()) }
        return outcomeOfWait(result)
    }

    private fun outcomeOfWait(result: NativeResult): ConnectOutcome? {
        if (result.isSuccess) return null
        return ConnectOutcome.Refused(refusalOf(waitRefusals, result.errorCode))
    }

    private fun refusalOf(refusals: Map<Long, IpcConnectResult>, code: Long): IpcConnectResult =
        refusals[code] ?: IpcConnectResult.Failed(Win32Error.errorOf(code))

    private fun channelOf(handle: Long, bufferSize: Int): NamedPipeChannel = NamedPipeChannel(
        api = api,
        handle = PipeHandle(api, handle),
        peer = identity.server(handle),
        bufferSize = bufferSize,
        dispatcher = dispatcher,
    )
}
