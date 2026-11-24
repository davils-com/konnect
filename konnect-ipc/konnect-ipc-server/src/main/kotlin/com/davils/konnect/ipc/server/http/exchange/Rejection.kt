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

package com.davils.konnect.ipc.server.http.exchange

import com.davils.konnect.ipc.server.http.exchange.response.ResponseHead
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteWriteChannel

private const val EMPTY: String = "0"

internal enum class Rejection(val status: HttpStatusCode, val reason: String) {
    Malformed(HttpStatusCode.BadRequest, "malformed"),
    HeaderTooLarge(HttpStatusCode.RequestHeaderFieldTooLarge, "header_too_large"),
    BodyTooLarge(HttpStatusCode.PayloadTooLarge, "body_too_large"),
    BodyTimeout(HttpStatusCode.RequestTimeout, "body_timeout"),
    Expectation(HttpStatusCode.ExpectationFailed, "expectation"),
    Encoding(HttpStatusCode.NotImplemented, "transfer_encoding"),
    ;

    suspend fun write(output: ByteWriteChannel) {
        val headers = HeadersBuilder()
        headers.append(name = HttpHeaders.ContentLength, value = EMPTY)
        ResponseHead.write(output = output, status = status, headers = headers, closes = true)
        output.flush()
    }
}
