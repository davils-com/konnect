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

package com.davils.konnect.ipc.server.http.connection

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.server.http.call.CallOrigin
import com.davils.konnect.ipc.server.http.exchange.Exchange
import com.davils.konnect.ipc.server.http.exchange.Rejection
import com.davils.konnect.ipc.server.http.exchange.head.HeadRead
import com.davils.konnect.ipc.server.http.exchange.head.HeadReader
import com.davils.konnect.ipc.server.http.exchange.request.Admission
import com.davils.konnect.ipc.server.http.exchange.request.RequestParser
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class ConnectionSession(private val connection: IpcConnection, private val context: SessionContext) {
    private val state = AtomicReference(SessionState.Busy)

    private val served = AtomicInt(0)

    private val origin: CallOrigin = CallOrigin(context.address, connection.peer)

    private val isClosing: Boolean
        get() = state.load() == SessionState.Closing || served.load() >= context.maxRequestsPerConnection

    suspend fun serve() {
        try {
            coroutineScope { exchanges(ConnectionWire(connection, this)) }
        } catch (expectedLoss: IOException) {
            context.telemetry.lost()
        } finally {
            connection.close()
        }
    }

    fun drain(): Boolean = state.exchange(SessionState.Closing) == SessionState.Idle

    private suspend fun exchanges(wire: ConnectionWire) {
        var isOpen = true
        while (isOpen && state.compareAndSet(expectedValue = SessionState.Busy, newValue = SessionState.Idle)) {
            val head = nextHead(wire)
            state.compareAndSet(expectedValue = SessionState.Idle, newValue = SessionState.Busy)
            isOpen = exchanged(head, wire)
        }
        wire.finish()
    }

    private suspend fun nextHead(wire: ConnectionWire): HeadRead {
        val head = withTimeoutOrNull(context.idleTimeout) { HeadReader.read(wire.input, context.maxHeaderSize) }
        return head ?: HeadRead.TimedOut
    }

    private suspend fun exchanged(head: HeadRead, wire: ConnectionWire): Boolean = when (head) {
        HeadRead.Ended -> false
        HeadRead.TimedOut -> false
        HeadRead.TooLarge -> rejected(wire, Rejection.HeaderTooLarge)
        is HeadRead.Complete -> parsed(head.bytes, wire)
    }

    private suspend fun parsed(bytes: ByteString, wire: ConnectionWire): Boolean =
        when (val admission = RequestParser.parse(bytes, context.maxRequestBodySize)) {
            is Admission.Rejected -> rejected(wire, admission.rejection)
            is Admission.Admitted -> admitted(admission, wire)
        }

    private suspend fun admitted(admission: Admission.Admitted, wire: ConnectionWire): Boolean {
        served.addAndFetch(1)
        val exchange = Exchange(admission = admission, wire = wire, context = context, origin = origin)
        return exchange.served { isClosing }
    }

    private suspend fun rejected(wire: ConnectionWire, rejection: Rejection): Boolean {
        rejection.write(wire.output)
        context.telemetry.rejected(rejection)
        return false
    }
}
