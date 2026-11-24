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

import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch

private const val HANDLE: Long = 3L
private const val SIZE: Int = 8
private const val PATH: String = "pipe"
private const val SDDL: String = "D:P"
private const val QUEUED: Long = 77L
private const val INSUFFICIENT_BUFFER: Long = 122L
private const val LONG_SID_PROCESS: Long = 300L

class FakeNamedPipeApiTest : FunSpec({
    context("the fake named pipe api") {
        test("answers with success unless told otherwise") {
            val api = FakeNamedPipeApi()

            val created = api.createServer(
                path = PATH,
                securityDescriptor = SDDL,
                bufferSize = SIZE,
                isFirstInstance = true,
            )
            val opened = api.open(PATH)

            opened.value shouldBe created.value + 1
            api.waitAvailable(PATH, timeoutMillis = 1) shouldBe NativeResult.success(0L)
            api.write(HANDLE, ByteBuffer.allocateDirect(SIZE), SIZE) shouldBe NativeResult.success(SIZE.toLong())
            api.written.single().size shouldBe SIZE
            api.awaitClient(HANDLE) shouldBe NativeResult.failure(Win32Error.OPERATION_ABORTED)
            api.read(
                HANDLE,
                ByteBuffer.allocateDirect(SIZE),
                SIZE,
            ) shouldBe NativeResult.failure(Win32Error.BROKEN_PIPE)
            api.created.single() shouldBe FakeCreatedPipe(PATH, securityDescriptor = SDDL, isFirstInstance = true)
            api.clientProcessId(HANDLE) shouldBe NativeResult.success(FakeNamedPipeApi.PEER_PROCESS)
            api.serverProcessId(HANDLE) shouldBe NativeResult.success(FakeNamedPipeApi.PEER_PROCESS)
            api.close(HANDLE) shouldBe NativeResult.success(0L)
            api.closed shouldBe listOf(HANDLE)
        }

        test("prefers the results that were queued") {
            val api = FakeNamedPipeApi()
            api.queueCreate(QUEUED)
            api.queueAwait(0L)
            api.queueOpen(QUEUED)
            api.queueWait(-Win32Error.SEM_TIMEOUT)
            api.queueWrite(0L)
            api.queueWrite(-Win32Error.NO_DATA)
            api.queueInbound(byteArrayOf(1, 2))

            val created = api.createServer(
                path = PATH,
                securityDescriptor = SDDL,
                bufferSize = SIZE,
                isFirstInstance = false,
            )

            created shouldBe NativeResult.fromEncoded(QUEUED)
            api.awaitClient(HANDLE) shouldBe NativeResult.success(0L)
            api.open(PATH) shouldBe NativeResult.fromEncoded(QUEUED)
            api.waitAvailable(PATH, timeoutMillis = 1) shouldBe NativeResult.failure(Win32Error.SEM_TIMEOUT)
            api.write(HANDLE, ByteBuffer.allocateDirect(SIZE), 0) shouldBe NativeResult.success(0L)
            api.write(HANDLE, ByteBuffer.allocateDirect(SIZE), SIZE) shouldBe NativeResult.failure(Win32Error.NO_DATA)
            api.emptyWrites shouldBe 1
            api.read(HANDLE, ByteBuffer.allocateDirect(SIZE), SIZE) shouldBe NativeResult.success(2L)
        }

        test("reads the failure and the peer it was told") {
            val api = FakeNamedPipeApi()
            api.readFailure = 0L
            api.peerProcessId = -Win32Error.ACCESS_DENIED

            api.read(HANDLE, ByteBuffer.allocateDirect(SIZE), SIZE) shouldBe NativeResult.success(0L)
            api.clientProcessId(HANDLE) shouldBe NativeResult.failure(Win32Error.ACCESS_DENIED)
        }

        test("names the users of its processes as the native library does") {
            val api = FakeNamedPipeApi(
                sids = mapOf(
                    FakeNamedPipeApi.CURRENT_PROCESS to FakeNamedPipeApi.CURRENT_SID,
                    LONG_SID_PROCESS to "S-1-" + "1".repeat(SIZE),
                ),
            )
            val buffer = ByteBuffer.allocateDirect(FakeNamedPipeApi.CURRENT_SID.length * 2)

            api.processUserSid(FakeNamedPipeApi.CURRENT_PROCESS, buffer) shouldBe
                NativeResult.success(FakeNamedPipeApi.CURRENT_SID.length.toLong())
            api.processUserSid(LONG_SID_PROCESS, ByteBuffer.allocateDirect(SIZE)) shouldBe
                NativeResult.failure(INSUFFICIENT_BUFFER)
            api.processUserSid(FakeNamedPipeApi.PEER_PROCESS, buffer) shouldBe
                NativeResult.failure(Win32Error.ACCESS_DENIED)
        }

        test("blocks until it is cancelled") {
            val api = FakeNamedPipeApi(blocker = CountDownLatch(1))

            api.cancel(HANDLE)

            api.read(HANDLE, ByteBuffer.allocateDirect(SIZE), SIZE) shouldBe
                NativeResult.failure(Win32Error.OPERATION_ABORTED)
            api.awaitClient(HANDLE) shouldBe NativeResult.failure(Win32Error.OPERATION_ABORTED)
            api.cancelled shouldContain HANDLE
        }
    }

    context("created pipes") {
        test("are equal when their path, descriptor and instance flag are") {
            val pipe = FakeCreatedPipe(path = PATH, securityDescriptor = SDDL, isFirstInstance = true)

            pipe shouldBe FakeCreatedPipe(path = PATH, securityDescriptor = SDDL, isFirstInstance = true)
            pipe.hashCode() shouldBe FakeCreatedPipe(
                path = PATH,
                securityDescriptor = SDDL,
                isFirstInstance = true,
            ).hashCode()
            pipe shouldNotBe FakeCreatedPipe(path = "other", securityDescriptor = SDDL, isFirstInstance = true)
            pipe shouldNotBe FakeCreatedPipe(path = PATH, securityDescriptor = "D:", isFirstInstance = true)
            pipe shouldNotBe FakeCreatedPipe(path = PATH, securityDescriptor = SDDL, isFirstInstance = false)
            pipe.equals(PATH) shouldBe false
            pipe.toString() shouldBe "FakeCreatedPipe(path=pipe, securityDescriptor=D:P, isFirstInstance=true)"
        }
    }
})
