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

internal const val LINE_END: String = "\r\n"

internal const val STATUS_OK: String = "200 OK"

internal const val PONG: String = "OK"

internal const val MALFORMED_HEAD: String = "The head of the response is malformed"

internal const val MALFORMED_BODY: String = "The chunks of the body of the response are malformed"

internal const val BODY_CUT_OFF: String =
    "The server closed the connection before the body of its response was complete"

internal val ok: String = answer(STATUS_OK, "Content-Length: 2", body = PONG)

internal fun head(vararg lines: String): String = lines.joinToString(LINE_END, postfix = LINE_END + LINE_END)

internal fun lines(vararg lines: String): String = lines.joinToString(separator = "") { line -> line + LINE_END }

internal fun answer(status: String, vararg headers: String, body: String = ""): String {
    val lines = listOf("HTTP/1.1 $status") + headers.asList()
    return lines.joinToString(LINE_END, postfix = LINE_END + LINE_END) + body
}

internal fun chunk(text: String): String = lines(text.length.toString(radix = 16), text)

internal fun lastChunk(): String = lines("0", "")
