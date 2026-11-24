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
import com.davils.konnect.ipc.core.transport.windows.NamedPipeChannel
import com.davils.konnect.ipc.core.transport.windows.PipeHandle
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.IpcListener
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.blocking
import com.davils.kore.core.platform.NativeResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private val notListening: AcceptOutcome = AcceptOutcome.Refused(IpcAcceptResult.NotListening)

@OptIn(ExperimentalAtomicApi::class)
internal class NamedPipeListener(
    private val api: NamedPipeApi,
    private val instances: PipeInstances,
    private val identity: PipeIdentity,
    first: PipeHandle,
    private val dispatcher: CoroutineDispatcher,
) : IpcListener {
    private val open = AtomicBoolean(true)

    private val pending = AtomicReference<PipeHandle?>(first)

    override suspend fun accept(): AcceptOutcome {
        val instance = pendingInstance() ?: return refusal()
        return instance.use(ifClosed = notListening) { value ->
            val result = awaited(instance, value)
            outcomeOf(instance, result)
        }
    }

    override fun close() {
        if (!open.compareAndSet(expectedValue = true, newValue = false)) return
        pending.exchange(null)?.close()
    }

    private suspend fun awaited(instance: PipeHandle, value: Long): NativeResult =
        blocking(dispatcher, onCancel = { discard(instance) }) { api.awaitClient(value) }

    private fun outcomeOf(instance: PipeHandle, result: NativeResult): AcceptOutcome {
        val code = result.errorCode
        if (!result.isSuccess && code != Win32Error.PIPE_CONNECTED) return failed(instance, code)
        return connected(instance)
    }

    private fun connected(instance: PipeHandle): AcceptOutcome {
        pending.compareAndSet(expectedValue = instance, newValue = null)
        prepareNext()
        val channel = NamedPipeChannel(
            api = api,
            handle = instance,
            peer = identity.client(instance.value),
            bufferSize = instances.bufferSize,
            dispatcher = dispatcher,
        )
        return AcceptOutcome.Accepted(channel)
    }

    private fun failed(instance: PipeHandle, code: Long): AcceptOutcome {
        discard(instance)
        if (!open.load()) return notListening
        return AcceptOutcome.Refused(IpcAcceptResult.Failed(Win32Error.errorOf(code)))
    }

    private fun discard(instance: PipeHandle) {
        if (!pending.compareAndSet(expectedValue = instance, newValue = null)) return
        instance.close()
    }

    private fun prepareNext() {
        val next = instances.create(isFirst = false)
        if (!next.isSuccess) return
        val instance = PipeHandle(api, next.value)
        if (open.load() && pending.compareAndSet(expectedValue = null, newValue = instance)) return
        instance.close()
    }

    private fun pendingInstance(): PipeHandle? {
        if (!open.load()) return null
        if (pending.load() == null) prepareNext()
        return pending.load()
    }

    private fun refusal(): AcceptOutcome {
        if (!open.load()) return notListening
        return AcceptOutcome.Refused(IpcAcceptResult.Failed(Win32Error.errorOf(Win32Error.PIPE_BUSY)))
    }
}
