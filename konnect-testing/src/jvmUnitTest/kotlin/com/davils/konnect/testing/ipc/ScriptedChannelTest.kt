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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val WAIT_MILLIS: Int = 20
private const val SIZE: Int = 4
private const val MORE: Int = 3

class ScriptedChannelTest : FunSpec({
    context("a scripted channel") {
        test("hands out its data in the reads it was given") {
            val channel = ScriptedChannel(
                reads = listOf(IpcChannelRead.Read(2), IpcChannelRead.Read(MORE), IpcChannelRead.Ended),
                data = byteArrayOf(1, 2, MORE.toByte()),
            )
            val target = ByteArray(SIZE)

            channel.read(target, offset = 0, length = SIZE) shouldBe IpcChannelRead.Read(2)
            channel.read(target, offset = 2, length = 2) shouldBe IpcChannelRead.Read(MORE)
            channel.read(target, offset = 0, length = SIZE) shouldBe IpcChannelRead.Ended
            target.toList() shouldBe listOf<Byte>(1, 2, MORE.toByte(), 0)
            channel.peer shouldBe TestIpc.CLIENT
        }

        test("waits for cancellation once its reads are used up") {
            val channel = ScriptedChannel()

            withTimeoutOrNull(WAIT_MILLIS.milliseconds) { channel.read(ByteArray(1), offset = 0, length = 1) } shouldBe
                null
        }

        test("answers every write with its result or waits for cancellation") {
            val written = ScriptedChannel()
            val waiting = ScriptedChannel(writeResult = null)

            written.write(ByteArray(1), offset = 0, length = 1) shouldBe IpcWriteResult.Written
            written.closeOutput() shouldBe IpcWriteResult.Written
            written.outputCloses shouldBe 1
            withTimeoutOrNull(WAIT_MILLIS.milliseconds) { waiting.write(ByteArray(1), offset = 0, length = 1) } shouldBe
                null
            withTimeoutOrNull(WAIT_MILLIS.milliseconds) { waiting.closeOutput() } shouldBe null
        }

        test("counts how often it was closed") {
            val channel = ScriptedChannel()

            channel.close()
            channel.close()

            channel.closes shouldBe 2
        }
    }
})
