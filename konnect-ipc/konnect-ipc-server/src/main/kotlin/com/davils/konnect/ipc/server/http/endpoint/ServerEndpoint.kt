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

package com.davils.konnect.ipc.server.http.endpoint

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.IpcServer
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult

internal class ServerEndpoint(private val server: IpcServer) : IpcEndpoint {
    override val address: IpcAddress get() = server.configuration.address

    override val telemetry: IpcTelemetry get() = server.configuration.telemetry

    override suspend fun bind(): IpcBindResult = server.bind()

    override suspend fun accept(): IpcAcceptResult = server.accept()

    override fun close() {
        server.close()
    }
}
