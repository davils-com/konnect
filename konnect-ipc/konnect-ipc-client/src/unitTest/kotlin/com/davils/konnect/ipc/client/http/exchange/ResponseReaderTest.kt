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

import com.davils.konnect.ipc.client.Limits
import com.davils.konnect.ipc.client.fixture.hostileText
import com.davils.konnect.ipc.client.fixture.shouldNotEcho
import com.davils.konnect.ipc.client.http.fixture.LINE_END
import com.davils.konnect.ipc.client.http.fixture.head
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import java.io.IOException

private const val CASES: Int = 1_000
private const val LIMIT: Int = Limits.MIN_RESPONSE_HEAD_SIZE
private const val LENGTH: Long = 7L
private const val STATUS_LINE: String = "HTTP/1.1 200 OK"
private const val PADDED: String = "X: "
private const val HEAD_END_LENGTH: Int = 4

private val reader = ResponseReader(LIMIT)

private suspend fun headOf(text: String): ResponseHead = reader.head(ByteReadChannel(text.encodeToByteArray()))

private suspend fun failureOf(bytes: ByteArray): String? = try {
    reader.head(ByteReadChannel(bytes))
    null
} catch (failure: IOException) {
    failure.message.orEmpty()
}

private suspend fun failureOf(text: String): String? = failureOf(text.encodeToByteArray())

private fun padded(size: Int): String {
    val start = STATUS_LINE.length + LINE_END.length + PADDED.length
    val padding = "a".repeat(size - start - HEAD_END_LENGTH)
    return head(STATUS_LINE, PADDED + padding)
}

class ResponseReaderTest : FunSpec({
    context("a head") {
        test("is read with its status, its version and its headers") {
            val head = headOf(head("HTTP/1.0 404 Gone Away", "Connection: Keep-Alive, Close", "Content-Length: 7"))

            head.status shouldBe HttpStatusCode.NotFound
            head.status.description shouldBe "Not Found"
            head.version shouldBe HttpProtocolVersion.HTTP_1_0
            head.contentLength shouldBe LENGTH
            head.isClosing shouldBe true
            head.isChunked shouldBe false
        }

        test("names its transfer codings in order") {
            val head = headOf(head(STATUS_LINE, "Transfer-Encoding: gzip", "Transfer-Encoding: Chunked"))

            head.transferCodings shouldBe listOf("gzip", "chunked")
            head.isChunked shouldBe true
            head.hasTransferEncoding shouldBe true
        }

        test("of a switch of the protocol is final") {
            headOf(head("HTTP/1.1 101 Switching Protocols")).status shouldBe HttpStatusCode.SwitchingProtocols
        }

        test("of exactly the limit is read") {
            headOf(padded(LIMIT)).status shouldBe HttpStatusCode.OK
        }

        test("one byte over the limit is refused") {
            failureOf(padded(LIMIT + 1)) shouldBe "The head of the response exceeds its limit"
        }

        test("ends only with an empty line of carriage return and line feed") {
            failureOf("$STATUS_LINE\n\n") shouldBe
                "The server closed the connection in the middle of the head of its response"
            failureOf(ByteArray(0)) shouldBe "The server closed the connection before it answered"
        }
    }

    context("a hostile head") {
        test("in its status line is read or refused without being quoted") {
            checkAll(CASES, hostileText) { text ->
                listOf(head("$text 200 OK"), head("HTTP/1.1 $text"), head("HTTP/1.1 200 $text")).forEach { response ->
                    failureOf(response).shouldNotEcho(text)
                }
            }
        }

        test("in its headers is read or refused without being quoted") {
            checkAll(CASES, hostileText) { text ->
                listOf(
                    head(STATUS_LINE, "$text: v"),
                    head(STATUS_LINE, "X: $text"),
                    head(STATUS_LINE, "Content-Length: $text"),
                    head(STATUS_LINE, text),
                ).forEach { response ->
                    failureOf(response).shouldNotEcho(text)
                }
            }
        }

        test("of raw bytes is read or refused without being quoted") {
            checkAll(CASES, hostileText) { text ->
                val bytes = text.toByteArray(Charsets.ISO_8859_1)

                failureOf(bytes + head().encodeToByteArray()).shouldNotEcho(text)
            }
        }
    }

    context("interim responses") {
        test("are refused beyond their limit") {
            val interim = head("HTTP/1.1 100 Continue").repeat(Limits.MAX_INTERIM_RESPONSES + 1)

            val failure = shouldThrow<IOException> { headOf(interim + head(STATUS_LINE)) }

            failure.message shouldBe "The server sent too many interim responses"
        }
    }
})
