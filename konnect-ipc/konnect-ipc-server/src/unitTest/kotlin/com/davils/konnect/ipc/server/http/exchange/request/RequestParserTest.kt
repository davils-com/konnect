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

import com.davils.konnect.ipc.server.fixture.hostileText
import com.davils.konnect.ipc.server.http.exchange.Rejection
import com.davils.konnect.ipc.server.http.exchange.fixture.lines
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll
import kotlinx.io.bytestring.encodeToByteString

private const val LIMIT: Long = 1_024
private const val LONG_LINE: Int = 9_000
private const val CASES: Int = 1_000

private suspend fun parsed(text: String): Admission = RequestParser.parse(text.encodeToByteString(), LIMIT)

class RequestParserTest : FunSpec({
    context("a head") {
        test("becomes a request with its method, target, version and headers") {
            val admitted = parsed(lines("PUT /a?b=c HTTP/1.0", "X-One: 1", "X-One: 2", "", ""))
                .shouldBeInstanceOf<Admission.Admitted>()

            admitted.head.method.value shouldBe "PUT"
            admitted.head.uri shouldBe "/a?b=c"
            admitted.head.query shouldBe "b=c"
            admitted.head.version shouldBe "HTTP/1.0"
            admitted.head.headers.getAll("x-one") shouldBe listOf("1", "2")
        }

        test("that ends before its blank line is malformed") {
            parsed(lines("GET / HTTP/1.1", "Host: konnect", "")) shouldBe Admission.Rejected(Rejection.Malformed)
        }

        test("with a version the engine does not speak is malformed") {
            parsed("GET / HTTP/3\r\n\r\n") shouldBe Admission.Rejected(Rejection.Malformed)
        }

        test("with a line beyond the limit of the parser is too large") {
            parsed(lines("GET / HTTP/1.1", "X: " + "a".repeat(LONG_LINE), "", "")) shouldBe
                Admission.Rejected(Rejection.HeaderTooLarge)
        }
    }

    context("hostile heads") {
        test("are admitted or refused, never thrown") {
            checkAll(CASES, hostileText, hostileText) { line, header ->
                val head = lines(line, "X-Probe: $header", "", "")

                val admission = parsed(head)

                (admission is Admission.Admitted || admission is Admission.Rejected) shouldBe true
            }
        }
    }
})
