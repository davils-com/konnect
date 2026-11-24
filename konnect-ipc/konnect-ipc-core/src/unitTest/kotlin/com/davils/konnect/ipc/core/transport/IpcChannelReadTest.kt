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
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val COUNT: Int = 3
private val error = IpcSystemError(code = 1L, description = "broken")

class IpcChannelReadTest : FunSpec({
    context("reads of a channel") {
        test("are equal when they read as many bytes") {
            IpcChannelRead.Read(COUNT) shouldBe IpcChannelRead.Read(COUNT)
            IpcChannelRead.Read(COUNT).hashCode() shouldBe COUNT
            IpcChannelRead.Read(COUNT) shouldNotBe IpcChannelRead.Read(1)
            IpcChannelRead.Read(COUNT).equals(COUNT) shouldBe false
            IpcChannelRead.Read(COUNT).toString() shouldBe "Read(count=3)"
        }

        test("are equal when they failed with the same error") {
            IpcChannelRead.Failed(error) shouldBe IpcChannelRead.Failed(error)
            IpcChannelRead.Failed(error).hashCode() shouldBe error.hashCode()
            IpcChannelRead.Failed(error).equals(error) shouldBe false
            IpcChannelRead.Failed(error).toString() shouldBe "Failed($error)"
        }
    }
})
