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

package com.davils.konnect.ipc.server.http.fixture

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.http.endpoint.IpcEndpoint
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.testing.ipc.TestIpc
import kotlinx.coroutines.channels.Channel

internal class ScriptedEndpoint(
    private val bindResult: IpcBindResult = IpcBindResult.Bound,
    override val telemetry: IpcTelemetry = IpcTelemetry.None,
) : IpcEndpoint {
    private val accepts: Channel<IpcAcceptResult> = Channel(Channel.UNLIMITED)

    override val address: IpcAddress = SERVICE

    @Volatile
    var closes: Int = 0
        private set

    override suspend fun bind(): IpcBindResult = bindResult

    override suspend fun accept(): IpcAcceptResult {
        val next = accepts.receiveCatching()
        return next.getOrNull() ?: IpcAcceptResult.NotListening
    }

    override fun close() {
        closes += 1
        accepts.close()
    }

    fun offer(result: IpcAcceptResult) {
        accepts.trySend(result)
    }

    fun accepted(peer: IpcPeer = TestIpc.CLIENT): IpcConnection {
        val pair = TestIpc.connections(client = peer)
        offer(IpcAcceptResult.Accepted(pair.server))
        return pair.client
    }
}
