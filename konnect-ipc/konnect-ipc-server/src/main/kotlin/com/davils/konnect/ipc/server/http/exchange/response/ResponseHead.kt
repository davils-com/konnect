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

package com.davils.konnect.ipc.server.http.exchange.response

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.RequestResponseBuilder
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writePacket

private const val VERSION: String = "HTTP/1.1"
private const val CLOSE: String = "close"

internal object ResponseHead {
    fun closes(headers: HeadersBuilder): Boolean = headers[HttpHeaders.Connection].equals(CLOSE, ignoreCase = true)

    suspend fun write(output: ByteWriteChannel, status: HttpStatusCode, headers: HeadersBuilder, closes: Boolean) {
        val builder = RequestResponseBuilder()
        try {
            builder.responseLine(VERSION, status.value, status.description)
            headers.entries().forEach { (name, values) ->
                lines(builder = builder, name = name, values = values, isClosing = closes)
            }
            if (closes) builder.headerLine(name = HttpHeaders.Connection, value = CLOSE)
            builder.emptyLine()
            builder.build().use { packet -> output.writePacket(packet) }
        } finally {
            builder.release()
        }
    }

    private fun lines(builder: RequestResponseBuilder, name: String, values: List<String>, isClosing: Boolean) {
        val isReplaced = isClosing && name.equals(HttpHeaders.Connection, ignoreCase = true)
        if (isReplaced) return
        values.forEach { value -> builder.headerLine(name = name, value = value) }
    }
}
