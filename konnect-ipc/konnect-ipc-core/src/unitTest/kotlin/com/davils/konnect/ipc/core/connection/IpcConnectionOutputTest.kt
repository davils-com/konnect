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
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.milliseconds

private const val BUFFER: Int = Limits.MIN_BUFFER_SIZE
private const val WAIT_MILLIS: Int = 20
private const val LATE: String = "late"

private val failure = IpcSystemError(code = 1, description = "broken")

class IpcConnectionOutputTest : FunSpec({
    context("closing the output") {
        test("ends what the other side reads and keeps what it still sends readable") {
            val connections = TestIpc.connections()

            connections.client.closeOutput() shouldBe IpcWriteResult.Written
            connections.server.write(LATE.encodeToByteString())
            connections.server.read() shouldBe IpcReadResult.Closed

            val answer = connections.client.read().shouldBeInstanceOf<IpcReadResult.Received>()
            answer.bytes.decodeToString() shouldBe LATE
            connections.client.isOpen shouldBe true
        }

        test("refuses every later write and happens once") {
            val client = TestIpc.connections().client

            client.closeOutput()

            client.isOutputOpen shouldBe false
            client.write(LATE.encodeToByteString()) shouldBe IpcWriteResult.Closed
            client.closeOutput() shouldBe IpcWriteResult.Closed
        }

        test("is refused on a closed connection") {
            val client = TestIpc.connections().client
            client.close()

            client.closeOutput() shouldBe IpcWriteResult.Closed
        }

        test("closes the connection when the system fails") {
            val channel = ScriptedChannel(writeResult = IpcWriteResult.Failed(failure))
            val connection = IpcConnection(channel, BUFFER)

            connection.closeOutput() shouldBe IpcWriteResult.Failed(failure)

            channel.outputCloses shouldBe 1
            connection.isOpen shouldBe false
        }

        test("waits as long as the channel does and closes the connection when the caller gives up") {
            val connection = IpcConnection(ScriptedChannel(writeResult = null), BUFFER)

            withTimeoutOrNull(WAIT_MILLIS.milliseconds) { connection.closeOutput() } shouldBe null

            connection.isOpen shouldBe false
        }
    }
})
