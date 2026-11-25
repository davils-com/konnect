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

import com.davils.konnect.ipc.server.http.exchange.ExchangeWire
import com.davils.konnect.ipc.server.http.exchange.response.ResponseCopy
import com.davils.konnect.ipc.server.http.exchange.response.ResponseFraming
import com.davils.konnect.ipc.server.http.exchange.response.ResponseHead
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.engine.BaseApplicationResponse
import io.ktor.server.response.ResponseHeaders
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

internal class IpcApplicationResponse(call: IpcApplicationCall, private val wire: ExchangeWire) :
    BaseApplicationResponse(call) {
    private val values: HeadersBuilder = HeadersBuilder()

    private var statusCode: HttpStatusCode = HttpStatusCode.OK

    private var body: Job = CompletableDeferred(Unit)

    private var isKeptAlive: Boolean = false

    private var isComplete: Boolean = true

    val isReusable: Boolean get() = isKeptAlive && isComplete && !body.isCancelled

    override val headers: ResponseHeaders = IpcResponseHeaders(values)

    override fun setStatus(statusCode: HttpStatusCode) {
        this.statusCode = statusCode
    }

    override suspend fun responseChannel(): ByteWriteChannel {
        val framing = sentHead()
        val sink = ByteChannel()
        body = wire.scope.async { ResponseCopy.copy(framing = framing, body = sink, output = wire.output) }
        return sink
    }

    override suspend fun respondFromBytes(bytes: ByteArray) {
        val framing = sentHead()
        if (framing.hasBody) wire.output.writeFully(bytes)
        wire.output.flush()
    }

    override suspend fun respondNoContent(content: OutgoingContent.NoContent) {
        sentHead()
        wire.output.flush()
    }

    override suspend fun respondUpgrade(upgrade: OutgoingContent.ProtocolUpgrade) {
        ResponseHead.write(output = wire.output, status = statusCode, headers = values, closes = false)
        wire.output.flush()
        val context = wire.scope.coroutineContext
        val switched = upgrade.upgrade(
            input = wire.input,
            output = wire.output,
            engineContext = context,
            userContext = context,
        )
        switched.join()
    }

    override suspend fun respondOutgoingContent(content: OutgoingContent) {
        isComplete = false
        super.respondOutgoingContent(content)
        body.join()
        isComplete = true
    }

    private suspend fun sentHead(): ResponseFraming {
        val framing = ResponseFraming.of(wire.isHead, values)
        val isClosing = wire.isClosing || !framing.isDelimited || ResponseHead.closes(values)
        isKeptAlive = !isClosing
        ResponseHead.write(output = wire.output, status = statusCode, headers = values, closes = isClosing)
        return framing
    }
}
