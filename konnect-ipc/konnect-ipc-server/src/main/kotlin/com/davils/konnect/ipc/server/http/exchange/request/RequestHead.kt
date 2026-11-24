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

package com.davils.konnect.ipc.server.http.exchange.request

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion

private const val QUERY: Char = '?'
private const val SEPARATOR: Char = ','
private const val CONTINUE: String = "100-continue"
private const val CLOSE: String = "close"
private const val KEEP_ALIVE: String = "keep-alive"

internal data class RequestHead(
    val method: HttpMethod,
    val uri: String,
    val version: String,
    val headers: Headers,
) {
    val query: String get() = uri.substringAfter(QUERY, missingDelimiterValue = "")

    val isHead: Boolean get() = method == HttpMethod.Head

    val isContinueExpected: Boolean get() = headers[HttpHeaders.Expect].equals(CONTINUE, ignoreCase = true)

    val isPersistent: Boolean
        get() {
            val values = headers.getAll(HttpHeaders.Connection).orEmpty()
            val tokens = values.flatMap { value -> value.split(SEPARATOR) }
            val options = tokens.mapTo(mutableSetOf()) { token -> token.trim().lowercase() }
            val isCurrent = HttpProtocolVersion.parse(version) == HttpProtocolVersion.HTTP_1_1
            return CLOSE !in options && (isCurrent || KEEP_ALIVE in options)
        }
}
