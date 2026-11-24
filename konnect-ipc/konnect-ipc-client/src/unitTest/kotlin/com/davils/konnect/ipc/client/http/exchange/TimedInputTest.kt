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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import java.io.IOException

private const val WAIT: Long = 20L
private const val TIMED_OUT: String = "timed out"

class TimedInputTest : FunSpec({
    context("a timed input") {
        test("is its source without a timeout") {
            val source = ByteReadChannel("abc")

            TimedInput.of(source, timeoutMillis = null) { IOException(TIMED_OUT) } shouldBeSameInstanceAs source
        }

        test("answers at once when the bytes are there and passes on the state of its source") {
            val source = ByteReadChannel("abc")
            val input = TimedInput.of(source, timeoutMillis = WAIT) { IOException(TIMED_OUT) }

            input.awaitContent(1) shouldBe true
            input.isClosedForRead shouldBe false
            input.closedCause shouldBe null
        }

        test("fails with its own failure when the source stays silent") {
            val source = ByteChannel()
            val input = TimedInput.of(source, timeoutMillis = WAIT) { IOException(TIMED_OUT) }

            shouldThrow<IOException> { input.awaitContent(1) }.message shouldBe TIMED_OUT
        }

        test("cancels its source") {
            val source = ByteChannel()
            val input = TimedInput.of(source, timeoutMillis = WAIT) { IOException(TIMED_OUT) }

            input.cancel(IOException("gone"))

            source.isClosedForRead shouldBe true
        }
    }
})
