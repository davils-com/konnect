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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val HANDLE: Long = 9L
private const val USED: String = "used"
private const val CLOSED: String = "closed"

class PipeHandleTest : FunSpec({
    context("a handle in use") {
        test("is released only after its last user left") {
            val api = FakeNamedPipeApi()
            val handle = PipeHandle(api, HANDLE)

            handle.use(ifClosed = CLOSED) {
                handle.close()
                api.cancelled shouldBe listOf(HANDLE)
                api.closed.shouldBeEmpty()
                USED
            } shouldBe USED

            api.closed shouldBe listOf(HANDLE)
        }
    }

    context("a handle used twice at once") {
        test("is released only after both users left") {
            val api = FakeNamedPipeApi()
            val handle = PipeHandle(api, HANDLE)

            handle.use(ifClosed = CLOSED) {
                handle.use(ifClosed = CLOSED) {
                    handle.close()
                    USED
                }
                api.closed.shouldBeEmpty()
                USED
            } shouldBe USED

            api.closed shouldBe listOf(HANDLE)
        }
    }

    context("a closed handle") {
        test("is released at once and refuses new users") {
            val api = FakeNamedPipeApi()
            val handle = PipeHandle(api, HANDLE)

            handle.close()
            handle.close()

            handle.isOpen shouldBe false
            handle.use(ifClosed = CLOSED) { USED } shouldBe CLOSED
            api.closed shouldBe listOf(HANDLE)
            api.cancelled shouldBe listOf(HANDLE)
        }
    }
})
