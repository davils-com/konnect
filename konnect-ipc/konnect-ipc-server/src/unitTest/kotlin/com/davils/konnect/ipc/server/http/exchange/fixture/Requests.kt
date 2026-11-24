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

package com.davils.konnect.ipc.server.http.exchange.fixture

private const val CRLF: String = "\r\n"
private const val HEX: Int = 16

internal fun lines(vararg parts: String): String = parts.joinToString(separator = CRLF)

internal fun request(line: String, vararg headers: String, body: String = ""): String {
    val head = (listOf(line) + headers).joinToString(separator = CRLF, postfix = CRLF)
    return head + CRLF + body
}

internal fun get(path: String, vararg headers: String): String = request("GET $path HTTP/1.1", *headers)

internal fun post(path: String, body: String, vararg headers: String): String =
    request("POST $path HTTP/1.1", "Content-Length: ${body.encodeToByteArray().size}", *headers, body = body)

internal fun chunked(vararg parts: String): String {
    val chunks = parts.joinToString(separator = "") { part -> "${part.length.toString(HEX)}$CRLF$part$CRLF" }
    return chunks + "0$CRLF$CRLF"
}
