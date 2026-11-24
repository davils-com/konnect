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
import io.ktor.http.HttpHeaders

private const val CONTINUE: String = "100-continue"
private const val CHUNKED: String = "chunked"
private const val INVALID: Long = -1L

internal object RequestAdmission {
    fun of(head: RequestHead, maxBodySize: Long): Admission {
        val expectation = head.headers[HttpHeaders.Expect]
        val isUnknown = expectation != null && !expectation.equals(CONTINUE, ignoreCase = true)
        if (isUnknown) return Admission.Rejected(Rejection.Expectation)
        return framed(head, maxBodySize)
    }

    private fun framed(head: RequestHead, maxBodySize: Long): Admission {
        val encodings = head.headers.getAll(HttpHeaders.TransferEncoding) ?: return lengthOf(head, maxBodySize)
        return encoded(head, encodings)
    }

    private fun encoded(head: RequestHead, encodings: List<String>): Admission {
        val encoding = encodings.singleOrNull()
        val refusals = listOf(
            Rejection.Malformed to (HttpHeaders.ContentLength in head.headers),
            Rejection.Encoding to !encoding.equals(CHUNKED, ignoreCase = true),
        )
        return admitted(head, refusals) { BodyFraming.Chunked }
    }

    private fun lengthOf(head: RequestHead, maxBodySize: Long): Admission {
        val headers = head.headers
        val values = headers.getAll(HttpHeaders.ContentLength) ?: return Admission.Admitted(head, BodyFraming.Empty)
        val declared = values.toSet()
        val length = declared.singleOrNull()?.let(::lengthOrNull) ?: INVALID
        val refusals = listOf(
            Rejection.Malformed to (length < 0),
            Rejection.BodyTooLarge to (length > maxBodySize),
        )
        return admitted(head, refusals) { framingOf(length) }
    }

    private fun lengthOrNull(text: String): Long? {
        val isDigits = text.isNotEmpty() && text.all { character -> character in '0'..'9' }
        if (!isDigits) return null
        return text.toLongOrNull()
    }

    private fun framingOf(length: Long): BodyFraming {
        if (length == 0L) return BodyFraming.Empty
        return BodyFraming.Length(length)
    }

    private fun admitted(
        head: RequestHead,
        refusals: List<Pair<Rejection, Boolean>>,
        framing: () -> BodyFraming,
    ): Admission {
        val refusal = refusals.firstOrNull { (_, applies) -> applies }?.first
        if (refusal != null) return Admission.Rejected(refusal)
        return Admission.Admitted(head, framing())
    }
}
