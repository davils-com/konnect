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

package com.davils.konnect.ipc.core.connection

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.encodeToByteString

private const val CODE: Long = 109L
private const val BROKEN: String = "broken"
private const val SECRET: String = "secret"

private val error = IpcSystemError(code = CODE, description = BROKEN)

class IpcResultsTest : FunSpec({
    context("system errors") {
        test("are equal when their code and description are") {
            error shouldBe IpcSystemError(CODE, BROKEN)
            error.hashCode() shouldBe IpcSystemError(CODE, BROKEN).hashCode()
            error shouldNotBe IpcSystemError(code = null, description = BROKEN)
            error shouldNotBe IpcSystemError(CODE, description = "gone")
            error.equals(BROKEN) shouldBe false
            IpcSystemError(code = null, description = BROKEN).hashCode() shouldBe
                IpcSystemError(code = null, description = BROKEN).hashCode()
        }

        test("show their code and description") {
            error.toString() shouldBe "IpcSystemError(code=109, description=broken)"
            IpcSystemError(code = null, description = BROKEN).toString() shouldBe
                "IpcSystemError(code=none, description=broken)"
        }
    }

    context("read results") {
        test("are equal when they carry the same bytes or error") {
            val received = IpcReadResult.Received(SECRET.encodeToByteString())

            received shouldBe IpcReadResult.Received(SECRET.encodeToByteString())
            received.hashCode() shouldBe SECRET.encodeToByteString().hashCode()
            received.equals(SECRET) shouldBe false
            IpcReadResult.Failed(error) shouldBe IpcReadResult.Failed(error)
            IpcReadResult.Failed(error).hashCode() shouldBe error.hashCode()
            IpcReadResult.Failed(error).equals(error) shouldBe false
        }

        test("never show the bytes that were received") {
            IpcReadResult.Received(SECRET.encodeToByteString()).toString() shouldBe "Received(size=6)"
            IpcReadResult.Failed(error).toString() shouldBe "Failed($error)"
        }
    }

    context("write results") {
        test("are equal when they carry the same error") {
            IpcWriteResult.Failed(error) shouldBe IpcWriteResult.Failed(error)
            IpcWriteResult.Failed(error).hashCode() shouldBe error.hashCode()
            IpcWriteResult.Failed(error).equals(error) shouldBe false
            IpcWriteResult.Failed(error).toString() shouldBe "Failed($error)"
        }
    }
})
