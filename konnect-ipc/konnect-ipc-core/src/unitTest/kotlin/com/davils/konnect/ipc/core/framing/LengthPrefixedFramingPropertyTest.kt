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
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.io.bytestring.ByteString

private const val CASES: Int = 200
private const val LARGEST: Int = 4096
private const val LIMIT: Int = 1024
private const val HEADER: Int = 4
private const val BYTE_MASK: Int = 0xFF

private fun hostileHeader(header: ByteArray): IpcConnection = IpcConnection(
    ScriptedChannel(reads = listOf(IpcChannelRead.Read(HEADER)), data = header),
    Limits.MIN_BUFFER_SIZE,
)

private fun sizeOf(header: ByteArray): Long =
    header.fold(0L) { size, byte -> size shl Byte.SIZE_BITS or (byte.toInt() and BYTE_MASK).toLong() }

class LengthPrefixedFramingPropertyTest : FunSpec({
    context("length prefixed frames") {
        test("arrive exactly as they were sent") {
            checkAll(CASES, Arb.byteArray(Arb.int(0..LARGEST), Arb.byte())) { bytes ->
                val connections = TestIpc.connections()
                val sender = connections.client.framed(IpcFraming.lengthPrefixed())
                val receiver = connections.server.framed(IpcFraming.lengthPrefixed())

                sender.send(ByteString(bytes)) shouldBe IpcWriteResult.Written

                receiver.receive() shouldBe IpcFrameResult.Received(ByteString(bytes))
            }
        }

        test("announced by a hostile header beyond the limit are refused before any body is read") {
            checkAll(CASES, Arb.byteArray(Arb.int(HEADER..HEADER), Arb.byte())) { announced ->
                val header = byteArrayOf(1) + announced.copyOfRange(fromIndex = 1, toIndex = HEADER)
                val size = sizeOf(header)
                val connection = hostileHeader(header)

                val result = IpcFraming.lengthPrefixed(LIMIT).receive(connection)

                val refused = result.shouldBeInstanceOf<IpcFrameResult.TooLarge>()
                refused.size shouldBe size
                refused.size shouldBeGreaterThan LIMIT.toLong()
                connection.isOpen shouldBe false
            }
        }
    }
})
