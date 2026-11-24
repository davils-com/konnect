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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.connection.IpcReadResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.io.bytestring.encodeToByteString

private const val PING: String = "ping"
private const val PONG: String = "pong"

class TestIpcTest : FunSpec({
    context("test connections") {
        test("see each other as their peers") {
            val connections = TestIpc.connections()

            connections.server.peer shouldBe TestIpc.CLIENT
            connections.client.peer shouldBe TestIpc.SERVER
        }

        test("carry bytes in both directions") {
            val connections = TestIpc.connections()

            connections.server.write(PING.encodeToByteString())
            connections.client.write(PONG.encodeToByteString())

            connections.client.read() shouldBe IpcReadResult.Received(PING.encodeToByteString())
            connections.server.read() shouldBe IpcReadResult.Received(PONG.encodeToByteString())
        }

        test("refuse a buffer outside of the limits") {
            shouldThrow<IllegalArgumentException> { TestIpc.connections(bufferSize = 0) }
        }

        test("are equal when they hold the same connections") {
            val connections = TestIpc.connections()
            val other = TestIpc.connections()
            val same = TestIpcConnections(server = connections.server, client = connections.client)

            connections shouldBe same
            connections.hashCode() shouldBe same.hashCode()
            connections shouldNotBe TestIpcConnections(server = connections.server, client = other.client)
            connections shouldNotBe TestIpcConnections(server = other.server, client = connections.client)
            connections.equals(connections.server) shouldBe false
            connections.toString() shouldStartWith "TestIpcConnections(server=IpcConnection("
        }
    }
})
