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

package com.davils.konnect.ipc.client

import com.davils.konnect.ipc.client.fixture.isUnix
import com.davils.konnect.ipc.client.fixture.socketPath
import com.davils.konnect.ipc.client.fixture.withThreads
import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.connection.IpcReadResult
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

private const val END: Int = -1

private val message: ByteArray = byteArrayOf(1, 2, 3)

private fun listeningAt(address: IpcAddress.UnixSocket): ServerSocketChannel {
    val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
    server.bind(UnixDomainSocketAddress.of(address.path))
    return server
}

class UnixClientTest : FunSpec({
    context("a client of a unix domain socket").config(enabledIf = { isUnix }) {
        test("connects, knows the user of the server and writes to it") {
            val address = IpcAddress.UnixSocket(socketPath())
            listeningAt(address).use { server ->
                withThreads { dispatcher ->
                    val result = ipcClient(dispatcher) { this.address = address }.connect()
                    val connection = result.shouldBeInstanceOf<IpcConnectResult.Connected>().connection
                    val accepted = server.accept()
                    val received = ByteBuffer.allocate(message.size)

                    connection.peer.user shouldBe IpcUser.UnixUser(IpcPlatform.environment.userName)
                    connection.write(ByteString(message)) shouldBe IpcWriteResult.Written
                    while (received.hasRemaining()) accepted.read(received)
                    received.array().toList() shouldBe message.toList()
                    connection.close()
                    accepted.close()
                }
            }
        }

        test("closes its output, so the server reads the end and can still answer") {
            val address = IpcAddress.UnixSocket(socketPath())
            listeningAt(address).use { server ->
                withThreads { dispatcher ->
                    val result = ipcClient(dispatcher) { this.address = address }.connect()
                    val connection = result.shouldBeInstanceOf<IpcConnectResult.Connected>().connection
                    val accepted = server.accept()

                    connection.closeOutput() shouldBe IpcWriteResult.Written
                    accepted.read(ByteBuffer.allocate(1)) shouldBe END
                    accepted.write(ByteBuffer.wrap(message))

                    val answer = connection.readExactly(message.size)
                    answer.shouldBeInstanceOf<IpcReadResult.Received>().bytes shouldBe ByteString(message)
                    connection.close()
                    accepted.close()
                }
            }
        }

        test("finds no server where nobody listens") {
            withThreads { dispatcher ->
                val address = IpcAddress.UnixSocket(socketPath())

                ipcClient(dispatcher) { this.address = address }.connect() shouldBe IpcConnectResult.NotFound
            }
        }

        test("rejects a server it does not trust") {
            val address = IpcAddress.UnixSocket(socketPath())
            listeningAt(address).use { server ->
                withThreads { dispatcher ->
                    val distrustful = ipcClient(dispatcher) {
                        this.address = address
                        trust {
                            isCurrentUserAllowed = false
                            allow(IpcUser.UnixUser("konnect-nobody"))
                        }
                    }

                    distrustful.connect().shouldBeInstanceOf<IpcConnectResult.ServerRejected>()
                }
                server.isOpen shouldBe true
            }
        }
    }
})
