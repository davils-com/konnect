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

import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.fixture.alicePeer
import com.davils.konnect.ipc.core.fixture.malloryPeer
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val CHUNK: Int = 2
private const val LAST: Byte = 3

class MemoryChannelTest : FunSpec({
    context("a pair of channels") {
        test("sees the other side as its peer") {
            val (first, second) = memoryChannels(first = alicePeer, second = malloryPeer)

            first.peer shouldBe malloryPeer
            second.peer shouldBe alicePeer
        }

        test("delivers a write in as many reads as the reader asks for") {
            val (first, second) = memoryChannels(first = alicePeer, second = malloryPeer)
            val target = ByteArray(CHUNK)

            first.write(byteArrayOf(1, 2, LAST), offset = 0, length = CHUNK + 1) shouldBe IpcWriteResult.Written

            second.read(target, offset = 0, length = CHUNK) shouldBe IpcChannelRead.Read(CHUNK)
            second.read(target, offset = 0, length = CHUNK) shouldBe IpcChannelRead.Read(1)
            target[0] shouldBe LAST
        }

        test("ends for both sides once one side closes") {
            val (first, second) = memoryChannels(first = alicePeer, second = malloryPeer)

            first.close()

            second.read(ByteArray(1), offset = 0, length = 1) shouldBe IpcChannelRead.Ended
            first.write(byteArrayOf(1), offset = 0, length = 1) shouldBe IpcWriteResult.Closed
        }

        test("ends what the other side reads once one side closes its output, and only once") {
            val (first, second) = memoryChannels(first = alicePeer, second = malloryPeer)

            first.closeOutput() shouldBe IpcWriteResult.Written
            first.closeOutput() shouldBe IpcWriteResult.Closed

            second.read(ByteArray(1), offset = 0, length = 1) shouldBe IpcChannelRead.Ended
        }
    }
})
