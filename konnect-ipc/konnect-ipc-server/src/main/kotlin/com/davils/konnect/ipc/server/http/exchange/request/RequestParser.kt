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

import com.davils.konnect.ipc.server.http.exchange.Rejection
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.Request
import io.ktor.http.cio.parseRequest
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.TooLongLineException
import kotlinx.io.bytestring.ByteString
import kotlin.coroutines.cancellation.CancellationException

private const val TAB: Byte = 9
private const val LF: Byte = 10
private const val CR: Byte = 13
private const val SPACE: Byte = 32
private const val TILDE: Byte = 126

internal object RequestParser {
    suspend fun parse(bytes: ByteString, maxBodySize: Long): Admission {
        val raw = bytes.toByteArray()
        if (!raw.all(::isHeadByte)) return Admission.Rejected(Rejection.Malformed)
        return parsed(raw, maxBodySize)
    }

    private suspend fun parsed(raw: ByteArray, maxBodySize: Long): Admission = try {
        val head = parseRequest(ByteReadChannel(raw))?.use(::headOf)
        admitted(head, maxBodySize)
    } catch (expectedCancellation: CancellationException) {
        throw expectedCancellation
    } catch (expectedTooLong: TooLongLineException) {
        Admission.Rejected(Rejection.HeaderTooLarge)
    } catch (expectedMalformed: IllegalStateException) {
        Admission.Rejected(Rejection.Malformed)
    } catch (expectedMalformed: IllegalArgumentException) {
        Admission.Rejected(Rejection.Malformed)
    }

    private fun isHeadByte(byte: Byte): Boolean = byte == TAB || byte == LF || byte == CR || byte in SPACE..TILDE

    private fun admitted(head: RequestHead?, maxBodySize: Long): Admission {
        if (head == null) return Admission.Rejected(Rejection.Malformed)
        return RequestAdmission.of(head, maxBodySize)
    }

    private fun headOf(request: Request): RequestHead = RequestHead(
        method = request.method,
        uri = request.uri.toString(),
        version = request.version.toString(),
        headers = headersOf(request.headers),
    )

    private fun headersOf(map: HttpHeadersMap): Headers {
        val builder = HeadersBuilder()
        map.offsets().forEach { offset ->
            val name = map.nameAtOffset(offset)
            val value = map.valueAtOffset(offset)
            builder.append(name = name.toString(), value = value.toString())
        }
        return builder.build()
    }
}
