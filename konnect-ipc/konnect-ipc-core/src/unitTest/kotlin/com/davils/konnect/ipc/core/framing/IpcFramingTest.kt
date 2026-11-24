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

package com.davils.konnect.ipc.core.framing

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val SMALL_LIMIT: Int = 8
private const val HEADER: Int = 4
private const val BUFFER: Int = Limits.MIN_BUFFER_SIZE
private val failure = IpcSystemError(code = 2, description = "gone")

private val announced: ByteArray = byteArrayOf(0, 0, 0, 1)

private fun scripted(vararg reads: IpcChannelRead): IpcConnection =
    IpcConnection(ScriptedChannel(reads = reads.toList(), data = announced), BUFFER)

class IpcFramingTest : FunSpec({
    context("length prefixed frames") {
        test("carry their size as four bytes in network order") {
            val framing = IpcFraming.lengthPrefixed()

            framing.encode("ab".encodeToByteString()) shouldBe
                ByteString(0, 0, 0, 2, 'a'.code.toByte(), 'b'.code.toByte())
            framing.toString() shouldContain "maxFrameSize=${Limits.DEFAULT_MAX_FRAME_SIZE}"
        }

        test("that are too large are refused and close the connection") {
            val connections = TestIpc.connections()
            connections.client.framed(IpcFraming.lengthPrefixed()).send(ByteString(ByteArray(SMALL_LIMIT + 1)))
            val receiver = connections.server.framed(IpcFraming.lengthPrefixed(SMALL_LIMIT))

            receiver.receive() shouldBe IpcFrameResult.TooLarge(size = SMALL_LIMIT + 1L, maxFrameSize = SMALL_LIMIT)
            receiver.isOpen shouldBe false
            shouldThrow<IllegalArgumentException> {
                IpcFraming.lengthPrefixed(SMALL_LIMIT).encode(ByteString(ByteArray(SMALL_LIMIT + 1)))
            }
            shouldThrow<IllegalArgumentException> { IpcFraming.lengthPrefixed(0) }
        }

        test("report a connection that closed or failed in the header") {
            val framing = IpcFraming.lengthPrefixed()

            framing.receive(scripted(IpcChannelRead.Ended)) shouldBe IpcFrameResult.Closed
            framing.receive(scripted(IpcChannelRead.Failed(failure))) shouldBe IpcFrameResult.Failed(failure)
        }

        test("report a connection that closed or failed in the body") {
            val framing = IpcFraming.lengthPrefixed()
            val header = IpcChannelRead.Read(HEADER)

            framing.receive(scripted(header, IpcChannelRead.Ended)) shouldBe IpcFrameResult.Closed
            framing.receive(scripted(header, IpcChannelRead.Failed(failure))) shouldBe IpcFrameResult.Failed(failure)
        }
    }

    context("a framed connection") {
        test("shares the peer and closes the connection beneath") {
            val server = TestIpc.connections().server
            val framed = server.framed(IpcFraming.lengthPrefixed())

            framed.peer shouldBe TestIpc.CLIENT
            framed.connection shouldBe server
            framed.framing.toString() shouldContain "LengthPrefixedFraming"
            framed.close()
            server.isOpen shouldBe false
            framed.toString() shouldContain "LengthPrefixedFraming"
        }

        test("sends what the framing encodes") {
            val connections = TestIpc.connections()
            val sender = connections.client.framed(IpcFraming.lengthPrefixed())

            sender.send("ab".encodeToByteString()) shouldBe IpcWriteResult.Written
        }
    }

    context("frame results") {
        test("are equal when they carry the same frame, sizes or error") {
            val frame = "ab".encodeToByteString()

            IpcFrameResult.Received(frame) shouldBe IpcFrameResult.Received(frame)
            IpcFrameResult.Received(frame).hashCode() shouldBe frame.hashCode()
            IpcFrameResult.Received(frame).equals(frame) shouldBe false
            IpcFrameResult.Received(frame).toString() shouldBe "Received"
            IpcFrameResult.Received(frame = null).hashCode() shouldBe 0
            IpcFrameResult.TooLarge(size = 2L, maxFrameSize = 1) shouldBe IpcFrameResult.TooLarge(2L, 1)
            IpcFrameResult.TooLarge(2L, 1).hashCode() shouldBe IpcFrameResult.TooLarge(2L, 1).hashCode()
            IpcFrameResult.TooLarge(2L, 1) shouldBe IpcFrameResult.TooLarge(2L, 1)
            (IpcFrameResult.TooLarge(2L, 1) == IpcFrameResult.TooLarge(HEADER.toLong(), 1)) shouldBe false
            (IpcFrameResult.TooLarge(2L, 1) == IpcFrameResult.TooLarge(2L, 2)) shouldBe false
            IpcFrameResult.TooLarge(2L, 1).equals(frame) shouldBe false
            IpcFrameResult.TooLarge(2L, 1).toString() shouldBe "TooLarge(size=2, maxFrameSize=1)"
            IpcFrameResult.Failed(failure) shouldBe IpcFrameResult.Failed(failure)
            IpcFrameResult.Failed(failure).hashCode() shouldBe failure.hashCode()
            IpcFrameResult.Failed(failure).equals(failure) shouldBe false
            IpcFrameResult.Failed(failure).toString() shouldBe "Failed($failure)"
        }
    }
})
