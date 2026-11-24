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
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode

internal class ResponseHead(
    val status: HttpStatusCode,
    val version: HttpProtocolVersion,
    val headers: Headers,
    val contentLength: Long?,
    val transferCodings: List<String>,
    val isClosing: Boolean,
) {
    val isInterim: Boolean get() = status.value < FIRST_FINAL && status != HttpStatusCode.SwitchingProtocols

    val isChunked: Boolean get() = transferCodings.lastOrNull() == CHUNKED

    val hasTransferEncoding: Boolean get() = transferCodings.isNotEmpty()

    companion object {
        const val CHUNKED: String = "chunked"

        const val FIRST_FINAL: Int = 200
    }
}
