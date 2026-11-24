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

package com.davils.konnect.ipc.server.http.exchange.head

import com.davils.konnect.ipc.server.http.exchange.fixture.lines
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.utils.io.ByteReadChannel

private const val LIMIT: Int = 64
private const val LINE_FEED_HEAD: Int = 27
private const val REQUEST_LINE: String = "GET / HTTP/1.1"
private const val HOST: String = "Host: kide"

private suspend fun readOf(text: String, limit: Int = LIMIT): HeadRead = HeadReader.read(ByteReadChannel(text), limit)

class HeadReaderTest : FunSpec({
    context("a head") {
        test("ends at the first blank line and skips the blank lines before it") {
            val read = readOf("\r\n\n" + lines(REQUEST_LINE, HOST, "", "rest")).shouldBeInstanceOf<HeadRead.Complete>()

            read.bytes.toByteArray().decodeToString() shouldBe lines(REQUEST_LINE, HOST, "", "")
        }

        test("may end its lines with a line feed alone") {
            val text = listOf(REQUEST_LINE, HOST, "", "").joinToString(separator = "\n")

            readOf(text).shouldBeInstanceOf<HeadRead.Complete>().bytes.size shouldBe LINE_FEED_HEAD
        }

        test("that grows beyond the limit is too large") {
            readOf(lines(REQUEST_LINE, "X: " + "a".repeat(LIMIT), "", "")) shouldBe HeadRead.TooLarge
        }

        test("that is cut off or missing ends the connection") {
            readOf(lines(REQUEST_LINE, "Host")) shouldBe HeadRead.Ended
            readOf("") shouldBe HeadRead.Ended
            readOf(lines("", "", "")) shouldBe HeadRead.Ended
        }
    }
})
