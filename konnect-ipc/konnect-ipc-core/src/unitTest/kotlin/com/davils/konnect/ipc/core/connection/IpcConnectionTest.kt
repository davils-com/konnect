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

package com.davils.konnect.ipc.core.connection

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.fixture.alicePeer
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val BUFFER: Int = Limits.MIN_BUFFER_SIZE
private const val PARTS: Int = 3
private const val HELLO: String = "hello"
private const val PART: String = "ab"
private const val PAYLOAD: String = "x"
private val failure = IpcSystemError(code = 1, description = "broken")

private fun connectionOf(channel: ScriptedChannel, onClose: () -> Unit = {}): IpcConnection =
    IpcConnection(channel, BUFFER, onClose)

class IpcConnectionTest : FunSpec({
    context("creating") {
        test("refuses a buffer outside of the limits") {
            listOf(Limits.MIN_BUFFER_SIZE - 1, Limits.MAX_BUFFER_SIZE + 1).forEach { size ->
                shouldThrow<IllegalArgumentException> { IpcConnection(ScriptedChannel(), size) }
            }
        }
    }

    context("reading") {
        test("returns what the other side wrote") {
            val connections = TestIpc.connections()

            connections.client.write(HELLO.encodeToByteString()) shouldBe IpcWriteResult.Written

            connections.server.read() shouldBe IpcReadResult.Received(HELLO.encodeToByteString())
        }

        test("exactly collects a message the other side wrote in parts") {
            val connections = TestIpc.connections()
            repeat(PARTS) { connections.client.write(PART.encodeToByteString()) }

            connections.server.readExactly(PARTS * 2) shouldBe
                IpcReadResult.Received(PART.repeat(PARTS).encodeToByteString())
            connections.server.readExactly(0) shouldBe IpcReadResult.Received(ByteString())
        }

        test("closes the connection once the other side has gone") {
            val closed = ScriptedChannel(reads = listOf(IpcChannelRead.Ended))
            val connection = connectionOf(closed)

            connection.read() shouldBe IpcReadResult.Closed
            connection.isOpen shouldBe false
            connection.read() shouldBe IpcReadResult.Closed
            connectionOf(ScriptedChannel(reads = listOf(IpcChannelRead.Ended))).readExactly(1) shouldBe
                IpcReadResult.Closed
        }

        test("closes the connection when the system fails") {
            val failing = ScriptedChannel(reads = listOf(IpcChannelRead.Failed(failure)))

            connectionOf(failing).read() shouldBe IpcReadResult.Failed(failure)
            failing.closes shouldBe 1
            val failingExactly = ScriptedChannel(
                reads = listOf(IpcChannelRead.Read(1), IpcChannelRead.Failed(failure)),
            )
            connectionOf(failingExactly).readExactly(2) shouldBe IpcReadResult.Failed(failure)
        }

        test("refuses to read nothing, too much or a negative amount") {
            val connection = connectionOf(ScriptedChannel(alicePeer))

            shouldThrow<IllegalArgumentException> { connection.read(0) }
            shouldThrow<IllegalArgumentException> { connection.read(Limits.MAX_BUFFER_SIZE + 1) }
            shouldThrow<IllegalArgumentException> { connection.readExactly(-1) }
        }

        test("closes the connection when the reader is cancelled") {
            val channel = ScriptedChannel(alicePeer)
            val connection = connectionOf(channel)

            coroutineScope {
                val reading = launch { connection.read() }
                yield()
                reading.cancelAndJoin()
            }

            connection.isOpen shouldBe false
            channel.closes shouldBe 1
        }
    }

    context("writing") {
        test("closes the connection when the other side has gone or the system fails") {
            listOf(IpcWriteResult.Closed, IpcWriteResult.Failed(failure)).forEach { result ->
                val connection = connectionOf(ScriptedChannel(writeResult = result))

                connection.write(PAYLOAD.encodeToByteString()) shouldBe result
                connection.isOpen shouldBe false
                connection.write(PAYLOAD.encodeToByteString()) shouldBe IpcWriteResult.Closed
            }
        }

        test("closes the connection when the writer is cancelled") {
            val connection = connectionOf(ScriptedChannel(writeResult = null))

            coroutineScope {
                val writing = async { connection.write(PAYLOAD.encodeToByteString()) }
                yield()
                writing.cancel(CancellationException("stop"))
                writing.join()
            }

            connection.isOpen shouldBe false
        }
    }

    context("closing") {
        test("happens once and tells the owner") {
            var released = 0
            val channel = ScriptedChannel(alicePeer)
            val connection = connectionOf(channel) { released += 1 }

            connection.close()
            connection.close()

            channel.closes shouldBe 1
            released shouldBe 1
            connection.peer shouldBe alicePeer
            connection.bufferSize shouldBe BUFFER
            connection.toString() shouldContain "open=false"
        }
    }
})
