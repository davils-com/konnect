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

package com.davils.konnect.ipc.client.http.fixture

import com.davils.konnect.ipc.client.http.connection.HttpConnection
import com.davils.konnect.ipc.core.connection.IpcConnection
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.Request
import io.ktor.http.cio.decodeChunked
import io.ktor.http.cio.parseRequest
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val CHUNKED: String = "chunked"

internal class Peer(private val connection: IpcConnection, scope: CoroutineScope) {
    private val wire: HttpConnection = HttpConnection(connection, scope)

    suspend fun request(): ReceivedRequest {
        val request = checkNotNull(parseRequest(wire.input)) { "The engine sent no request" }
        val (line, headers) = request.use(::headOf)
        return ReceivedRequest(line, headers, bodyOf(headers).decodeToString())
    }

    suspend fun rest(): String = wire.input.toByteArray().decodeToString()

    suspend fun answer(text: String) {
        connection.write(text.encodeToByteString())
    }

    suspend fun answer(bytes: ByteArray) {
        connection.write(ByteString(bytes))
    }

    fun close() {
        wire.close()
    }

    private fun headOf(request: Request): Pair<String, Map<String, String>> =
        "${request.method.value} ${request.uri}" to headersOf(request.headers)

    private suspend fun bodyOf(headers: Map<String, String>): ByteArray {
        val length = headers["content-length"]?.toInt()
        if (length != null) return wire.input.readByteArray(length)
        return chunkedOf(isChunked = headers["transfer-encoding"] == CHUNKED)
    }

    private suspend fun chunkedOf(isChunked: Boolean): ByteArray {
        if (!isChunked) return ByteArray(0)
        val decoded = ByteChannel()
        decodeChunked(input = wire.input, out = decoded)
        decoded.flushAndClose()
        return decoded.toByteArray()
    }

    private fun headersOf(map: HttpHeadersMap): Map<String, String> = map.offsets().associate { offset ->
        val name = map.nameAtOffset(offset).toString()
        name.lowercase() to map.valueAtOffset(offset).toString()
    }
}
