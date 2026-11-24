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

import com.davils.konnect.ipc.core.transport.toSystemError
import com.davils.konnect.ipc.core.transport.unix.UnixChannel
import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.IpcListener
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.blocking
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.file.Path
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class UnixListener(
    private val server: UnixServerSocket,
    private val path: Path,
    private val staging: Path,
    private val api: UnixSocketApi,
    private val dispatcher: CoroutineDispatcher,
) : IpcListener {
    private val open = AtomicBoolean(true)

    override suspend fun accept(): AcceptOutcome = try {
        AcceptOutcome.Accepted(UnixChannel(nextSocket(), dispatcher))
    } catch (expectedClosed: ClosedChannelException) {
        AcceptOutcome.Refused(IpcAcceptResult.NotListening)
    } catch (expectedClosed: ClosedSelectorException) {
        AcceptOutcome.Refused(IpcAcceptResult.NotListening)
    } catch (expectedFailure: IOException) {
        AcceptOutcome.Refused(IpcAcceptResult.Failed(expectedFailure.toSystemError()))
    }

    override fun close() {
        if (!open.compareAndSet(expectedValue = true, newValue = false)) return
        quietly { server.close() }
        quietly { api.delete(path) }
        quietly { api.delete(staging) }
    }

    private suspend fun nextSocket(): UnixSocket {
        var socket: UnixSocket? = null
        while (socket == null) {
            if (!open.load()) throw ClosedChannelException()
            socket = blocking(dispatcher, onCancel = server::wakeup, onDiscard = ::discard) { server.accept() }
        }
        return socket
    }

    private fun discard(socket: UnixSocket?) {
        quietly { socket?.close() }
    }

    private inline fun quietly(action: () -> Unit) {
        try {
            action()
        } catch (expectedFailure: IOException) {
            return
        }
    }
}
