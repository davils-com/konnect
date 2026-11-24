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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.fixture.alicePeer
import com.davils.konnect.ipc.core.fixture.immediate
import com.davils.konnect.ipc.core.fixture.withThreads
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.milliseconds

private const val HANDLE: Long = 5L
private const val BUFFER: Int = Limits.MIN_BUFFER_SIZE
private const val TARGET: Int = 4
private const val MESSAGE: Int = BUFFER * 2 + 2
private const val WAIT_MILLIS: Int = 50
private const val UNEXPECTED: Long = 87L

private fun channelOf(api: FakeNamedPipeApi, dispatcher: CoroutineDispatcher = immediate()) = NamedPipeChannel(
    api = api,
    handle = PipeHandle(api, HANDLE),
    peer = alicePeer,
    bufferSize = BUFFER,
    dispatcher = dispatcher,
)

class NamedPipeChannelTest : FunSpec({
    context("creating") {
        test("refuses a buffer outside of the limits") {
            val api = FakeNamedPipeApi()

            listOf(Limits.MIN_BUFFER_SIZE - 1, Limits.MAX_BUFFER_SIZE + 1).forEach { size ->
                shouldThrow<IllegalArgumentException> {
                    NamedPipeChannel(
                        api = api,
                        handle = PipeHandle(api, HANDLE),
                        peer = alicePeer,
                        bufferSize = size,
                        dispatcher = immediate(),
                    )
                }
            }
        }
    }

    context("reading") {
        test("copies what arrived into the target") {
            val api = FakeNamedPipeApi()
            api.queueInbound(byteArrayOf(1, 2))
            val target = ByteArray(TARGET)

            channelOf(api).read(target, offset = 1, length = TARGET - 1) shouldBe IpcChannelRead.Read(2)

            target.toList() shouldBe listOf<Byte>(0, 1, 2, 0)
        }

        test("never copies more than was asked for") {
            val api = FakeNamedPipeApi()
            api.queueInbound(byteArrayOf(1, 2, TARGET.toByte()))
            val target = ByteArray(2)

            channelOf(api).read(target, offset = 0, length = 2) shouldBe IpcChannelRead.Read(2)
        }

        test("ends when the pipe is broken, empty or closed") {
            val api = FakeNamedPipeApi()
            val channel = channelOf(api)

            channel.read(ByteArray(TARGET), offset = 0, length = TARGET) shouldBe IpcChannelRead.Ended
            api.readFailure = 0L
            channel.read(ByteArray(TARGET), offset = 0, length = TARGET) shouldBe IpcChannelRead.Ended
            channel.close()
            channel.read(ByteArray(TARGET), offset = 0, length = TARGET) shouldBe IpcChannelRead.Ended
        }

        test("fails on any other error") {
            val api = FakeNamedPipeApi()
            api.readFailure = -UNEXPECTED

            channelOf(api).read(ByteArray(TARGET), offset = 0, length = TARGET) shouldBe
                IpcChannelRead.Failed(Win32Error.errorOf(UNEXPECTED))
        }

        test("cancels the pending read when the caller gives up") {
            val api = FakeNamedPipeApi(blocker = CountDownLatch(1))

            withThreads { dispatcher ->
                withTimeoutOrNull(WAIT_MILLIS.milliseconds) {
                    channelOf(api, dispatcher).read(ByteArray(TARGET), offset = 0, length = TARGET)
                }
            }

            api.cancelled shouldContain HANDLE
        }
    }

    context("writing") {
        test("sends a message in pieces that fit the buffer") {
            val api = FakeNamedPipeApi()

            channelOf(api).write(ByteArray(MESSAGE), offset = 0, length = MESSAGE) shouldBe IpcWriteResult.Written

            api.written.map { piece -> piece.size } shouldBe listOf(BUFFER, BUFFER, 2)
        }

        test("reports a closed pipe and any other error") {
            val api = FakeNamedPipeApi()
            val channel = channelOf(api)
            api.queueWrite(-Win32Error.NO_DATA)
            api.queueWrite(-UNEXPECTED)
            api.queueWrite(0L)

            channel.write(ByteArray(1), offset = 0, length = 1) shouldBe IpcWriteResult.Closed
            channel.write(ByteArray(1), offset = 0, length = 1) shouldBe
                IpcWriteResult.Failed(Win32Error.errorOf(UNEXPECTED))
            channel.write(ByteArray(1), offset = 0, length = 1) shouldBe IpcWriteResult.Closed
            channel.close()
            channel.write(ByteArray(1), offset = 0, length = 1) shouldBe IpcWriteResult.Closed
        }
    }

    context("closing the output") {
        test("writes a message of no bytes, which the other side reads as the end") {
            val api = FakeNamedPipeApi()

            channelOf(api).closeOutput() shouldBe IpcWriteResult.Written

            api.emptyWrites shouldBe 1
        }

        test("reports a failure of the pipe and a pipe that is closed") {
            val api = FakeNamedPipeApi()
            val channel = channelOf(api)
            api.queueWrite(-UNEXPECTED)

            channel.closeOutput() shouldBe IpcWriteResult.Failed(Win32Error.errorOf(UNEXPECTED))
            channel.close()
            channel.closeOutput() shouldBe IpcWriteResult.Closed
        }
    }
})
