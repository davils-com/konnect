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

package com.davils.konnect.ipc.client.http.exchange

import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.Response

private const val CLOSE: String = "close"
private const val SEPARATOR: Char = ','
private val statuses: IntRange = 100..599
private val decimalLength: Regex = Regex("[0-9]{1,18}")
private val versions: Map<String, HttpProtocolVersion> = mapOf(
    "HTTP/1.1" to HttpProtocolVersion.HTTP_1_1,
    "HTTP/1.0" to HttpProtocolVersion.HTTP_1_0,
)

internal object ResponseHeads {
    fun of(response: Response): ResponseHead {
        val version = versions[response.version.toString()]
        require(version != null && response.status in statuses) { MALFORMED_HEAD }
        val headers = headersOf(response.headers)
        val contentLength = lengthOf(headers)
        val transferCodings = tokensOf(headers, HttpHeaders.TransferEncoding)
        require(contentLength == null || transferCodings.isEmpty()) { MALFORMED_HEAD }
        return ResponseHead(
            status = HttpStatusCode.fromValue(response.status),
            version = version,
            headers = headers,
            contentLength = contentLength,
            transferCodings = transferCodings,
            isClosing = CLOSE in tokensOf(headers, HttpHeaders.Connection),
        )
    }

    private fun headersOf(map: HttpHeadersMap): Headers {
        val builder = HeadersBuilder()
        map.offsets().forEach { offset ->
            val name = map.nameAtOffset(offset)
            val value = map.valueAtOffset(offset)
            builder.append(name = name.toString(), value = value.toString())
        }
        return builder.build()
    }

    private fun lengthOf(headers: Headers): Long? {
        val values = headers.getAll(HttpHeaders.ContentLength) ?: return null
        val distinct = values.toSet()
        val value = distinct.singleOrNull()
        require(value != null && decimalLength.matches(value)) { MALFORMED_HEAD }
        return value.toLong()
    }

    private fun tokensOf(headers: Headers, name: String): List<String> {
        val values = headers.getAll(name).orEmpty()
        val tokens = values.flatMap { value -> value.split(SEPARATOR) }
        return tokens.map { token -> token.trim().lowercase() }.filter(String::isNotEmpty)
    }
}
