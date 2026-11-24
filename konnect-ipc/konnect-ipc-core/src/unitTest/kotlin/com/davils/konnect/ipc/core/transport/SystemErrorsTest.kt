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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.ipc.core.transport

import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.fixture.hostileText
import com.davils.konnect.ipc.core.fixture.shouldNotEcho
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.file.AccessDeniedException

private const val CASES: Int = 1_000

class SystemErrorsTest : FunSpec({
    context("an i/o failure") {
        test("is described by its type, never by its message") {
            IOException("/home/alice/secret.sock").toSystemError() shouldBe
                IpcSystemError(code = null, description = "IOException")
            AccessDeniedException("/run/konnect/helper.sock").toSystemError() shouldBe
                IpcSystemError(code = null, description = "AccessDeniedException")
            ClosedChannelException().toSystemError() shouldBe
                IpcSystemError(code = null, description = "ClosedChannelException")
        }

        test("of an anonymous type is described as an i/o failure") {
            val anonymous = object : IOException("anonymous") {}

            anonymous.toSystemError().description shouldBe "IOException"
            anonymous.toSystemError().code.shouldBeNull()
        }

        test("never carries hostile text from its message into the description") {
            checkAll(CASES, hostileText) { text ->
                val error = IOException(text).toSystemError()

                error.description.shouldNotEcho(text)
                error.toString().shouldNotEcho(text)
            }
        }
    }
})
