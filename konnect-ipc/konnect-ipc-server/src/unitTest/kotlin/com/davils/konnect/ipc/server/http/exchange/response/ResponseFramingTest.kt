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

import com.davils.konnect.ipc.server.http.exchange.Rejection
import com.davils.konnect.ipc.server.http.exchange.fixture.lines
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HeadersBuilder
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray

private fun headersOf(vararg headers: Pair<String, String>): HeadersBuilder {
    val builder = HeadersBuilder()
    headers.forEach { (name, value) -> builder.append(name, value) }
    return builder
}

class ResponseFramingTest : FunSpec({
    context("the framing of an answer") {
        test("follows the request method, the encoding and the length in this order") {
            ResponseFraming.of(isHead = true, headersOf("Content-Length" to "4")) shouldBe ResponseFraming.Discarded
            ResponseFraming.of(isHead = false, headersOf("Transfer-Encoding" to "chunked")) shouldBe
                ResponseFraming.Chunked
            ResponseFraming.of(isHead = false, headersOf("Content-Length" to "4")) shouldBe ResponseFraming.Fixed
            ResponseFraming.of(isHead = false, headersOf()) shouldBe ResponseFraming.UntilClose
        }
    }

    context("the head of an answer") {
        test("closes when it says so in any case") {
            ResponseHead.closes(headersOf("Connection" to "Close")) shouldBe true
            ResponseHead.closes(headersOf("Connection" to "keep-alive")) shouldBe false
            ResponseHead.closes(headersOf()) shouldBe false
        }
    }

    context("a rejection") {
        test("says what is wrong with its status alone, in constant text, and closes the connection") {
            Rejection.entries.forEach { rejection ->
                val output = ByteChannel()

                rejection.write(output)
                output.flushAndClose()

                val status = "HTTP/1.1 ${rejection.status.value} ${rejection.status.description}"

                val expected = lines(status, "Content-Length: 0", "Connection: close", "", "")

                output.toByteArray().decodeToString() shouldBe expected
            }
        }
    }
})
