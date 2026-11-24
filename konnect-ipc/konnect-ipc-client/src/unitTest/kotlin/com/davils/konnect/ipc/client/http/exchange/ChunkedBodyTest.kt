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
import com.davils.konnect.ipc.client.http.fixture.BODY_CUT_OFF
import com.davils.konnect.ipc.client.http.fixture.MALFORMED_BODY
import com.davils.konnect.ipc.client.http.fixture.lines
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import java.io.IOException

private const val CASES: Int = 1_000
private const val TRAILERS: Int = 64
private const val TRAILER_REPEATS: Int = 3
private const val LAST: String = "0"

private val chunked = ChunkedBody(TRAILERS)

private suspend fun decoded(text: String): String {
    val output = ByteChannel()
    chunked.copy(ByteReadChannel(text.encodeToByteArray()), output)
    output.flushAndClose()
    return output.toByteArray().decodeToString()
}

private suspend fun failureOf(text: String): String? = try {
    decoded(text)
    null
} catch (failure: IOException) {
    failure.message.orEmpty()
}

class ChunkedBodyTest : FunSpec({
    context("a chunked body") {
        test("is decoded with extensions and trailers") {
            decoded(lines("3;name=value", "abc", "2 ; x", "de", LAST, "Trailer: yes", "")) shouldBe "abcde"
        }

        test("of no chunks is empty") {
            decoded(lines(LAST, "")) shouldBe ""
        }
    }

    context("a broken chunked body") {
        test("with a size that is no number, too long or empty is refused") {
            listOf("zz", "", "1234567890abcdef", "-1").forEach { size ->
                failureOf(lines(size)) shouldBe MALFORMED_BODY
            }
        }

        test("with a line that is too long or ends in a bare line feed is refused") {
            failureOf(lines("1;" + "x".repeat(Limits.MAX_CHUNK_LINE_LENGTH))) shouldBe MALFORMED_BODY
            failureOf("1\na" + lines("", LAST, "")) shouldBe MALFORMED_BODY
            failureOf("1\rx") shouldBe MALFORMED_BODY
        }

        test("without the end of a chunk is refused") {
            failureOf(lines("1") + "aXY" + lines(LAST, "")) shouldBe MALFORMED_BODY
            failureOf(lines("1") + "a\rX") shouldBe MALFORMED_BODY
        }

        test("that ends early is refused") {
            listOf(lines("5") + "ab", "5", lines("2") + "ab", lines(LAST)).forEach { body ->
                failureOf(body) shouldBe BODY_CUT_OFF
            }
        }

        test("with trailers beyond their limit is refused") {
            val trailer = "X: " + "a".repeat(TRAILERS / 2)

            val body = lines(LAST) + lines(trailer).repeat(TRAILER_REPEATS) + lines("")

            failureOf(body) shouldBe "The trailers of the response exceed their limit"
        }

        test("with hostile text is decoded or refused without being quoted") {
            checkAll(CASES, hostileText) { text ->
                listOf(lines(text), lines("1", "a") + text, lines(LAST, text, "")).forEach { body ->
                    failureOf(body).shouldNotEcho(text)
                }
            }
        }
    }
})
