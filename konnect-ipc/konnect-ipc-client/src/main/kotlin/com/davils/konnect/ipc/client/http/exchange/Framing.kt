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

import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode

private val emptyStatuses: Set<Int> = setOf(HttpStatusCode.NoContent.value, HttpStatusCode.NotModified.value)

internal enum class Framing(val isDelimited: Boolean) {
    Empty(isDelimited = true),
    Length(isDelimited = true),
    Chunked(isDelimited = true),
    UntilClose(isDelimited = false),
    Raw(isDelimited = false),
    ;

    companion object {
        fun of(method: HttpMethod, head: ResponseHead): Framing {
            val status = head.status
            val choices = listOf(
                Raw to (status == HttpStatusCode.SwitchingProtocols),
                Empty to (method == HttpMethod.Head || status.value in emptyStatuses),
                Chunked to head.isChunked,
                UntilClose to (head.hasTransferEncoding || head.contentLength == null),
                Length to true,
            )
            return choices.first { (_, applies) -> applies }.first
        }

        fun isReusable(framing: Framing, head: ResponseHead): Boolean {
            val isKeptAlive = head.version == HttpProtocolVersion.HTTP_1_1 && !head.isClosing
            return framing.isDelimited && isKeptAlive
        }
    }
}
