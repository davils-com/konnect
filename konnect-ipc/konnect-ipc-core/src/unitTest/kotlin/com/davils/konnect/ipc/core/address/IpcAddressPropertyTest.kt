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

package com.davils.konnect.ipc.core.address

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.fixture.ROOT
import com.davils.konnect.ipc.core.fixture.hostileText
import com.davils.konnect.ipc.core.fixture.shouldNotEcho
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import java.nio.file.InvalidPathException
import java.nio.file.Path

private const val CASES: Int = 1_000
private val NAME_ALPHABET: Regex = Regex("[a-z0-9._-]+")

class IpcAddressPropertyTest : FunSpec({
    context("names read from hostile text") {
        test("are either refused or hold only the characters of a name") {
            checkAll(CASES, hostileText) { text ->
                val value = IpcName.parseOrNull(text)?.value.orEmpty()

                value.length shouldBeLessThanOrEqual Limits.MAX_NAME_LENGTH
                (value.isEmpty() || NAME_ALPHABET.matches(value)) shouldBe true
            }
        }

        test("are refused with a message that never quotes the text") {
            checkAll(CASES, hostileText) { text ->
                val failure = runCatching { IpcName(text) }.exceptionOrNull()

                failure?.message.shouldNotEcho(text)
            }
        }
    }

    context("pipe names read from hostile text") {
        test("are either refused or hold no control character and no backslash within the limit") {
            checkAll(CASES, hostileText) { text ->
                val isValid = IpcAddress.WindowsPipe.isValid(text)
                val isPrintable = text.none { character -> character.isISOControl() || character == '\\' }

                val isShaped = text.length <= Limits.MAX_PIPE_NAME_LENGTH && isPrintable

                (!isValid || isShaped) shouldBe true
            }
        }

        test("are refused with a message that never quotes the text") {
            checkAll(CASES, hostileText) { text ->
                val failure = runCatching { IpcAddress.WindowsPipe(text) }.exceptionOrNull()

                failure?.message.shouldNotEcho(text)
            }
        }
    }

    context("socket paths read from hostile text") {
        test("are either refused or absolute, free of control characters and within the limit") {
            checkAll(CASES, hostileText) { text ->
                val path = pathOrNull(text)
                val shown = path?.toString().orEmpty()
                val isValid = path != null && IpcAddress.UnixSocket.isValid(path)
                val isFitting = shown.encodeToByteArray().size <= Limits.MAX_SOCKET_PATH_BYTES

                val isShaped = isFitting && shown.none(Char::isISOControl)

                (!isValid || isShaped) shouldBe true
            }
        }

        test("are refused with a message that never quotes the path") {
            checkAll(CASES, hostileText) { text ->
                val path = pathOrNull(text) ?: ROOT
                val failure = runCatching { IpcAddress.UnixSocket(path) }.exceptionOrNull()

                failure?.message.shouldNotEcho(path.toString())
            }
        }
    }
})

private fun pathOrNull(text: String): Path? = try {
    ROOT.resolve(text)
} catch (expected: InvalidPathException) {
    null
}
