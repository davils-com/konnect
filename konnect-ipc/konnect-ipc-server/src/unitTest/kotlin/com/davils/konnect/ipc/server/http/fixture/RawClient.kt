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

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcReadResult
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.Response
import io.ktor.http.cio.decodeChunked
import io.ktor.http.cio.parseResponse
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.toByteArray
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.bytestring.encodeToByteString

private const val CHUNKED: String = "chunked"
private const val FINAL: Int = 200

internal class RawClient(private val connection: IpcConnection, scope: CoroutineScope) {
    private val input: ByteReadChannel = scope.writer { pump(channel) }.channel

    suspend fun send(text: String) {
        connection.write(text.encodeToByteString())
    }

    suspend fun response(isHead: Boolean = false): RawResponse {
        val response = checkNotNull(parseResponse(input)) { "The engine answered nothing" }
        val (status, headers) = response.use(::headOf)
        return RawResponse(status, headers, bodyOf(headers, isHead || status < FINAL))
    }

    suspend fun exchange(request: String, isHead: Boolean = false): RawResponse {
        send(request)
        return response(isHead)
    }

    suspend fun rest(): String = input.toByteArray().decodeToString()

    suspend fun closeOutput() {
        connection.closeOutput()
    }

    fun close() {
        connection.close()
    }

    private fun headOf(response: Response): Pair<Int, Map<String, String>> = response.status to headersOf(
        response.headers,
    )

    private fun headersOf(map: HttpHeadersMap): Map<String, String> = map.offsets().associate { offset ->
        val name = map.nameAtOffset(offset).toString()
        name.lowercase() to map.valueAtOffset(offset).toString()
    }

    private suspend fun bodyOf(headers: Map<String, String>, isHead: Boolean): String {
        val length = headers["content-length"]?.toInt()
        if (isHead || length != null) return input.readByteArray(length.takeUnless { isHead } ?: 0).decodeToString()
        return chunkedOf(isChunked = headers["transfer-encoding"] == CHUNKED)
    }

    private suspend fun chunkedOf(isChunked: Boolean): String {
        if (!isChunked) return rest()
        val decoded = ByteChannel()
        decodeChunked(input, decoded)
        return decoded.toByteArray().decodeToString()
    }

    private suspend fun pump(channel: ByteWriteChannel) {
        var result = connection.read()
        while (result is IpcReadResult.Received) {
            channel.writeByteArray(result.bytes.toByteArray())
            channel.flush()
            result = connection.read()
        }
    }
}
