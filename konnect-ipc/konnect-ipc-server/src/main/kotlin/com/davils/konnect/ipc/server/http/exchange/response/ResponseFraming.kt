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

private const val CHUNKED: String = "chunked"

internal enum class ResponseFraming(val hasBody: Boolean, val isDelimited: Boolean) {
    Fixed(hasBody = true, isDelimited = true),
    Chunked(hasBody = true, isDelimited = true),
    UntilClose(hasBody = true, isDelimited = false),
    Discarded(hasBody = false, isDelimited = true),
    ;

    companion object {
        fun of(isHead: Boolean, headers: HeadersBuilder): ResponseFraming {
            val isChunked = headers[HttpHeaders.TransferEncoding].equals(CHUNKED, ignoreCase = true)
            val choices = listOf(
                Discarded to isHead,
                Chunked to isChunked,
                Fixed to headers.contains(HttpHeaders.ContentLength),
            )
            val choice = choices.firstOrNull { (_, applies) -> applies } ?: return UntilClose
            return choice.first
        }
    }
}
