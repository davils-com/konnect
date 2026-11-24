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

package com.davils.konnect.ipc.core

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcReadResult
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.fixture.isUnix
import com.davils.konnect.ipc.core.fixture.isWindows
import com.davils.konnect.ipc.core.fixture.socketPath
import com.davils.konnect.ipc.core.fixture.withThreads
import com.davils.konnect.ipc.core.framing.IpcFrameResult
import com.davils.konnect.ipc.core.framing.IpcFraming
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.ipc.core.transport.unix.UnixChannel
import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocket
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.encodeToByteString

private const val SIZE: Int = 4
private const val GREETING: String = "hello over a real socket"

private fun acceptedBy(server: UnixServerSocket): UnixSocket {
    var accepted: UnixSocket? = null
    while (accepted == null) accepted = server.accept()
    return accepted
}

class PlatformPrimitivesTest : FunSpec({
    context("unix domain sockets of the jdk").config(enabledIf = { isUnix }) {
        test("carry bytes and name the user on the other side") {
            val path = socketPath()
            val sockets = IpcPlatform.unixSockets
            val server = sockets.bind(path)
            val client = sockets.connect(path)
            val accepted = acceptedBy(server)

            withThreads { dispatcher ->
                val sending = UnixChannel(client, dispatcher)
                val receiving = UnixChannel(accepted, dispatcher)
                val target = ByteArray(SIZE)

                sending.write(ByteArray(SIZE), offset = 0, length = SIZE) shouldBe IpcWriteResult.Written
                receiving.read(target, offset = 0, length = SIZE) shouldBe IpcChannelRead.Read(SIZE)
                receiving.peer.user shouldBe IpcUser.UnixUser(IpcPlatform.environment.userName)
                sending.close()
                receiving.close()
            }
            server.close()
            sockets.delete(path)
        }

        test("carry a frame between two connections and end with the output of the sender") {
            val path = socketPath()
            val sockets = IpcPlatform.unixSockets
            val server = sockets.bind(path)
            val client = sockets.connect(path)
            val accepted = acceptedBy(server)

            withThreads { dispatcher ->
                val sender = IpcConnection(UnixChannel(client, dispatcher), Limits.MIN_BUFFER_SIZE)
                val receiver = IpcConnection(UnixChannel(accepted, dispatcher), Limits.MIN_BUFFER_SIZE)
                val framing = IpcFraming.lengthPrefixed()
                val frame = GREETING.encodeToByteString()

                sender.framed(framing).send(frame) shouldBe IpcWriteResult.Written
                receiver.framed(framing).receive() shouldBe IpcFrameResult.Received(frame)
                sender.closeOutput() shouldBe IpcWriteResult.Written
                receiver.read() shouldBe IpcReadResult.Closed
                sender.close()
            }
            server.close()
            sockets.delete(path)
        }
    }

    context("moving a bound socket of the jdk").config(enabledIf = { isUnix }) {
        test("keeps it listening at its new path and leaves nothing at the old one") {
            val staged = socketPath()
            val path = staged.resolveSibling("moved.sock")
            val sockets = IpcPlatform.unixSockets
            val server = sockets.bind(staged)

            sockets.move(source = staged, target = path)
            val client = sockets.connect(path)
            val accepted = acceptedBy(server)

            sockets.exists(staged) shouldBe false
            sockets.exists(path) shouldBe true
            accepted.peerUser() shouldBe IpcPlatform.environment.userName
            client.close()
            accepted.close()
            server.close()
            sockets.delete(path)
        }
    }

    context("the native named pipe library").config(enabledIf = { isWindows }) {
        test("is loaded from the packaged artifact") {
            IpcPlatform.namedPipes.api.shouldNotBeNull()
        }
    }
})
