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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.Headers
import io.ktor.http.HttpMethod

private const val LIMIT: Long = 10
private const val LENGTH: String = "Content-Length"
private const val FOUR: Long = 4

private fun headOf(vararg headers: Pair<String, String>, version: String = "HTTP/1.1"): RequestHead {
    val built = Headers.build { headers.forEach { (name, value) -> append(name, value) } }
    return RequestHead(method = HttpMethod.Post, uri = "/", version = version, headers = built)
}

private fun framingOf(vararg headers: Pair<String, String>): Admission = RequestAdmission.of(headOf(*headers), LIMIT)

class RequestAdmissionTest : FunSpec({
    context("the length of a body") {
        test("frames nothing without a declaration and nothing for zero") {
            framingOf() shouldBe Admission.Admitted(headOf(), BodyFraming.Empty)
            val zero = LENGTH to "0"
            framingOf(zero) shouldBe Admission.Admitted(headOf(zero), BodyFraming.Empty)
        }

        test("frames the declared bytes when every declaration agrees") {
            val first = LENGTH to "4"

            framingOf(first, first) shouldBe Admission.Admitted(headOf(first, first), BodyFraming.Length(FOUR))
        }

        test("refuses declarations that disagree, carry a sign, are no number or exceed the limit") {
            framingOf(LENGTH to "4", LENGTH to "5") shouldBe Admission.Rejected(Rejection.Malformed)
            framingOf(LENGTH to "-4") shouldBe Admission.Rejected(Rejection.Malformed)
            framingOf(LENGTH to "+4") shouldBe Admission.Rejected(Rejection.Malformed)
            framingOf(LENGTH to "") shouldBe Admission.Rejected(Rejection.Malformed)
            framingOf(LENGTH to "99999999999999999999") shouldBe Admission.Rejected(Rejection.Malformed)
            framingOf(LENGTH to "11") shouldBe Admission.Rejected(Rejection.BodyTooLarge)
        }
    }

    context("an encoding") {
        test("of chunks in any case frames chunks") {
            val chunked = "Transfer-Encoding" to "Chunked"

            framingOf(chunked) shouldBe Admission.Admitted(headOf(chunked), BodyFraming.Chunked)
        }

        test("declared twice is refused") {
            val chunked = "Transfer-Encoding" to "chunked"

            framingOf(chunked, chunked) shouldBe Admission.Rejected(Rejection.Encoding)
        }
    }

    context("an expectation") {
        test("of 100-continue in any case is admitted") {
            val expect = "Expect" to "100-Continue"

            framingOf(expect) shouldBe Admission.Admitted(headOf(expect), BodyFraming.Empty)
        }
    }

    context("a request") {
        test("is kept alive by HTTP/1.1 unless it asks to close and by HTTP/1.0 only when it asks to") {
            headOf().isPersistent shouldBe true
            headOf("Connection" to "Upgrade, Close").isPersistent shouldBe false
            headOf(version = "HTTP/1.0").isPersistent shouldBe false
            headOf("Connection" to "Keep-Alive", version = "HTTP/1.0").isPersistent shouldBe true
        }

        test("expects to continue only when it says so") {
            headOf().isContinueExpected shouldBe false
            headOf("Expect" to "100-continue").isContinueExpected shouldBe true
            headOf().isHead shouldBe false
        }
    }
})
