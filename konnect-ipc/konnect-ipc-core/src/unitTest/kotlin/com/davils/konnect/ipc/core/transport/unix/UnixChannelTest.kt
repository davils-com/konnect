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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.fixture.immediate
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.FakeUnixSocket
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.channels.ClosedChannelException

private const val BROKEN: String = "broken"
private const val SIZE: Int = 8
private const val CHUNK: Int = 3
private val broken = IpcSystemError(code = null, description = "IOException")

private fun channelOf(socket: UnixSocket): UnixChannel = UnixChannel(socket, immediate())

class UnixChannelTest : FunSpec({
    context("the peer") {
        test("is the user the socket reports") {
            channelOf(FakeUnixSocket()).peer shouldBe IpcPeer(IpcUser.UnixUser("alice"), processId = null)
        }

        test("is unknown when the socket reports no user or one that is no user name") {
            channelOf(FakeUnixSocket(peerName = null)).peer shouldBe IpcPeer.UNKNOWN
            channelOf(FakeUnixSocket(peerName = "a\nb")).peer shouldBe IpcPeer.UNKNOWN
        }

        test("is unknown when the platform does not support peer credentials") {
            val unsupported = object : UnixSocket by FakeUnixSocket() {
                override fun peerUser(): String = throw UnsupportedOperationException("SO_PEERCRED")
            }

            channelOf(unsupported).peer shouldBe IpcPeer.UNKNOWN
        }
    }

    context("reading") {
        test("returns how many bytes arrived and ends with the stream") {
            val channel = channelOf(FakeUnixSocket(reads = listOf(CHUNK, -1)))

            channel.read(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcChannelRead.Read(CHUNK)
            channel.read(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcChannelRead.Ended
        }

        test("ends on a closed socket and fails on any other problem without its message") {
            channelOf(FakeUnixSocket()).read(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcChannelRead.Ended
            val failing = channelOf(FakeUnixSocket(readFailure = IOException(BROKEN)))

            failing.read(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcChannelRead.Failed(broken)
        }
    }

    context("writing") {
        test("keeps writing until every byte is out") {
            val socket = FakeUnixSocket(writeChunk = CHUNK)

            channelOf(socket).write(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcWriteResult.Written

            socket.written shouldBe SIZE
        }

        test("reports a closed socket and any other problem") {
            val closed = channelOf(FakeUnixSocket(writeFailure = ClosedChannelException()))
            val failing = channelOf(FakeUnixSocket(writeFailure = IOException(BROKEN)))

            closed.write(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcWriteResult.Closed
            failing.write(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcWriteResult.Failed(broken)
        }
    }

    context("closing") {
        test("swallows a socket that fails to close") {
            val socket = FakeUnixSocket(closeFailure = IOException(BROKEN))

            channelOf(socket).close()

            socket.closes shouldBe 1
        }
    }

    context("closing the output") {
        test("shuts the output of the socket down and keeps it open") {
            val socket = FakeUnixSocket()

            channelOf(socket).closeOutput() shouldBe IpcWriteResult.Written

            socket.isOutputShut shouldBe true
            socket.closes shouldBe 0
        }

        test("reports a closed socket and any other failure") {
            channelOf(FakeUnixSocket(writeFailure = ClosedChannelException())).closeOutput() shouldBe
                IpcWriteResult.Closed
            channelOf(FakeUnixSocket(writeFailure = IOException(BROKEN))).closeOutput() shouldBe
                IpcWriteResult.Failed(broken)
        }
    }
})
