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

package com.davils.konnect.ipc.server.http.call

import com.davils.konnect.ipc.server.http.IpcHttpCallAttributes
import com.davils.konnect.ipc.server.http.exchange.ExchangeWire
import com.davils.konnect.ipc.server.http.exchange.request.RequestHead
import io.ktor.server.application.Application
import io.ktor.server.engine.BaseApplicationCall
import io.ktor.utils.io.ByteReadChannel
import kotlin.coroutines.CoroutineContext

internal class IpcApplicationCall(
    application: Application,
    head: RequestHead,
    body: ByteReadChannel,
    wire: ExchangeWire,
    origin: CallOrigin,
) : BaseApplicationCall(application) {
    override val coroutineContext: CoroutineContext = wire.scope.coroutineContext

    override val request: IpcApplicationRequest = IpcApplicationRequest(
        call = this,
        head = head,
        body = body,
        origin = origin,
    )

    override val response: IpcApplicationResponse = IpcApplicationResponse(this, wire)

    init {
        putResponseAttribute()
        attributes.put(IpcHttpCallAttributes.peer, origin.peer)
        attributes.put(IpcHttpCallAttributes.address, origin.address)
    }
}
