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

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

private const val SIZE: Int = 8
private const val CHUNK: Int = 3
private const val BROKEN: String = "broken"
private val directory: Path = Path.of("/tmp/konnect-alice")
private val socket: Path = directory.resolve("s.sock")
private val failure = IOException(BROKEN)

class FakeUnixTest : FunSpec({
    context("the fake environment") {
        test("names its user, its temporary directory and its variables") {
            val environment = FakeIpcEnvironment(variables = mapOf("HOME" to "/home/alice"))

            environment.userName shouldBe FakeIpcEnvironment.USER
            environment.temporaryDirectory shouldBe Path.of("/tmp")
            environment.variable("HOME") shouldBe "/home/alice"
            environment.variable("PATH").shouldBeNull()
        }
    }

    context("the fake unix socket api") {
        test("remembers paths, owners and permissions") {
            val api = FakeUnixSocketApi(existing = setOf(socket))
            val private = PosixFilePermissions.fromString("rw-------")

            api.createDirectories(directory)
            api.restrict(socket, private)
            api.delete(socket)

            api.exists(directory) shouldBe true
            api.exists(socket) shouldBe false
            api.isWritable(socket) shouldBe true
            api.owner(directory) shouldBe FakeIpcEnvironment.USER
            api.permissions[socket] shouldBe private
            api.deleted shouldBe listOf(socket)
            api.bind(socket) shouldBe api.server
            api.exists(socket) shouldBe true
            api.connect(socket).peerUser() shouldBe FakeIpcEnvironment.USER
        }

        test("moves a path to its target") {
            val staged = directory.resolve("staged")
            val api = FakeUnixSocketApi(existing = setOf(staged))

            api.move(source = staged, target = socket)

            api.exists(staged) shouldBe false
            api.exists(socket) shouldBe true
            api.moved shouldBe listOf(staged to socket)
        }

        test("fails where it was told to") {
            val api = FakeUnixSocketApi(
                directoryFailure = failure,
                bindFailure = failure,
                restrictFailure = failure,
                moveFailure = failure,
            )

            shouldThrow<IOException> { api.createDirectories(directory) }
            shouldThrow<IOException> { api.bind(socket) }
            shouldThrow<IOException> { api.restrict(socket, emptySet()) }
            shouldThrow<IOException> { api.move(source = directory, target = socket) }
            api.moved shouldBe emptyList()
            api.restrict(directory, emptySet())
            api.permissions[directory] shouldBe emptySet()
        }
    }

    context("the fake unix server socket") {
        test("hands out its sockets, wakes up and closes") {
            val socket = FakeUnixSocket()
            val server = FakeUnixServerSocket(listOf(socket, null))

            server.accept() shouldBe socket
            server.accept().shouldBeNull()
            server.wakeup()
            server.accept().shouldBeNull()
            shouldThrow<IOException> { server.close() }
            server.wakeups shouldBe 1
            server.closes shouldBe 1
            shouldThrow<IOException> { FakeUnixServerSocket(failure = failure).accept() }
        }

        test("closes quietly when told to") {
            val server = FakeUnixServerSocket(closeFailure = null)

            server.close()

            server.closes shouldBe 1
        }
    }

    context("the fake unix socket") {
        test("reads as it was told and then fails") {
            val socket = FakeUnixSocket(peerName = null, reads = listOf(CHUNK, 0, -1), readFailure = failure)
            val buffer = ByteBuffer.allocate(SIZE)

            socket.read(buffer) shouldBe CHUNK
            buffer.position() shouldBe CHUNK
            socket.read(buffer) shouldBe 0
            socket.read(buffer) shouldBe -1
            shouldThrow<IOException> { socket.read(buffer) }
            shouldThrow<IOException> { socket.peerUser() }
            shouldThrow<ClosedChannelException> { FakeUnixSocket().read(buffer) }
        }

        test("writes in chunks and shuts its output down") {
            val socket = FakeUnixSocket(writeChunk = CHUNK)
            val buffer = ByteBuffer.allocate(SIZE)

            socket.write(buffer) shouldBe CHUNK
            socket.written shouldBe CHUNK
            socket.shutdownOutput()
            socket.isOutputShut shouldBe true
            socket.close()
            socket.closes shouldBe 1
        }

        test("fails to write, shut down and close as it was told") {
            val socket = FakeUnixSocket(writeFailure = failure, closeFailure = failure)

            shouldThrow<IOException> { socket.write(ByteBuffer.allocate(SIZE)) }
            shouldThrow<IOException> { socket.shutdownOutput() }
            shouldThrow<IOException> { socket.close() }
            socket.closes shouldBe 1
        }
    }
})
