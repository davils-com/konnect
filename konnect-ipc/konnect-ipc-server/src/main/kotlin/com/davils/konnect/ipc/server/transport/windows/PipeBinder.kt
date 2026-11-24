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

import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.PipeHandle
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineDispatcher

private val takenNames: Set<Long> = setOf(Win32Error.ACCESS_DENIED, Win32Error.PIPE_BUSY)

internal class PipeBinder(
    private val api: NamedPipeApi,
    private val identity: PipeIdentity,
    private val dispatcher: CoroutineDispatcher,
) {
    fun bind(instances: PipeInstances): ListenOutcome {
        val first = instances.create(isFirst = true)
        if (!first.isSuccess) return ListenOutcome.Refused(refusalOf(first.errorCode))
        val listener = NamedPipeListener(
            api = api,
            instances = instances,
            identity = identity,
            first = PipeHandle(api, first.value),
            dispatcher = dispatcher,
        )
        return ListenOutcome.Listening(listener)
    }

    private fun refusalOf(code: Long): IpcBindResult {
        if (code in takenNames) return IpcBindResult.AddressInUse
        return IpcBindResult.Failed(Win32Error.errorOf(code))
    }
}
