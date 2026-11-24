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

@file:OptIn(InternalAPI::class)

package com.davils.konnect.ipc.client.http.exchange

import com.davils.konnect.ipc.client.http.IpcHttpUpgrade
import com.davils.konnect.ipc.client.http.connection.HttpConnection
import io.ktor.client.engine.mergeHeaders
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.cio.RequestResponseBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.readByteArray

private const val VERSION: String = "HTTP/1.1"
private const val CHUNK: Int = 16_384
private const val HEX: Int = 16
private const val UPGRADE: String = "Upgrade"
private val lineEnd: ByteArray = "\r\n".encodeToByteArray()
private val lastChunk: ByteArray = "0\r\n\r\n".encodeToByteArray()
private val lineBreaks: Set<Char> = setOf('\r', '\n')

private val framingHeaders: Set<String> = setOf(
    HttpHeaders.Host.lowercase(),
    HttpHeaders.ContentLength.lowercase(),
    HttpHeaders.TransferEncoding.lowercase(),
    HttpHeaders.Connection.lowercase(),
    HttpHeaders.Upgrade.lowercase(),
)

private val bodiless: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Delete, HttpMethod.Options)

internal class RequestWriter(private val connection: HttpConnection, private val scope: CoroutineScope) {
    suspend fun write(data: HttpRequestData, upgrade: IpcHttpUpgrade?) {
        val body = bodyOf(data.body)
        connection.write(headOf(data, body, upgrade))
        body.source?.let { source -> streamed(source, body.isChunked) }
    }

    private fun bodyOf(content: OutgoingContent): OutgoingBody = when (content) {
        is OutgoingContent.ByteArrayContent -> bytesOf(content.bytes())
        is OutgoingContent.NoContent -> OutgoingBody(length = 0L, source = null)
        is OutgoingContent.ReadChannelContent -> OutgoingBody(content.contentLength, content.readFrom())
        is OutgoingContent.WriteChannelContent -> OutgoingBody(content.contentLength, written(content))
        is OutgoingContent.ContentWrapper -> bodyOf(content.delegate())
        is OutgoingContent.ProtocolUpgrade -> throw IllegalArgumentException(FOREIGN_UPGRADE)
    }

    private fun bytesOf(bytes: ByteArray): OutgoingBody = OutgoingBody(bytes.size.toLong(), ByteReadChannel(bytes))

    private fun headOf(data: HttpRequestData, body: OutgoingBody, upgrade: IpcHttpUpgrade?): ByteArray {
        val builder = RequestResponseBuilder()
        builder.requestLine(method = data.method, uri = data.url.encodedPathAndQuery, version = VERSION)
        builder.headerLine(name = HttpHeaders.Host, value = data.url.host)
        mergeHeaders(data.headers, data.body) { name, value ->
            require(value.none(lineBreaks::contains)) { LINE_BREAK_IN_HEADER }
            if (name.lowercase() !in framingHeaders) builder.headerLine(name = name, value = value)
        }
        framingOf(data.method, body).forEach { (name, value) -> builder.headerLine(name = name, value = value) }
        upgrade?.let { switch ->
            builder.headerLine(name = HttpHeaders.Connection, value = UPGRADE)
            builder.headerLine(name = HttpHeaders.Upgrade, value = switch.protocol)
        }
        builder.emptyLine()
        return builder.build().use { packet -> packet.readByteArray() }
    }

    private fun framingOf(method: HttpMethod, body: OutgoingBody): List<Pair<String, String>> {
        if (body.source == null && method in bodiless) return emptyList()
        val chunked = HttpHeaders.TransferEncoding to ResponseHead.CHUNKED
        val known = body.length?.let { size -> HttpHeaders.ContentLength to size.toString() }
        return listOf(known ?: chunked)
    }

    private fun written(content: OutgoingContent.WriteChannelContent): ByteReadChannel {
        val job = scope.writer { content.writeTo(channel) }
        return job.channel
    }

    private suspend fun streamed(source: ByteReadChannel, isChunked: Boolean) {
        val buffer = ByteArray(CHUNK)
        var count = source.readAvailable(buffer)
        while (count > 0) {
            connection.write(framed(buffer, count, isChunked))
            count = source.readAvailable(buffer)
        }
        if (isChunked) connection.write(lastChunk)
    }

    private fun framed(buffer: ByteArray, count: Int, isChunked: Boolean): ByteArray {
        val bytes = buffer.copyOf(count)
        if (!isChunked) return bytes
        val size = count.toString(HEX).encodeToByteArray()
        return size + lineEnd + bytes + lineEnd
    }
}
