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

package com.davils.konnect.ipc.server.transport.windows

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.transport.windows.PipeHandle
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.milliseconds

private const val FIRST: Long = 1L
private const val BUFFER: Int = 1024
private const val PATH: String = """\\.\pipe\helper"""
private const val SDDL: String = "D:P"
private const val WAIT: Int = 50
private const val UNEXPECTED: Long = -87L

private fun listenerOf(api: FakeNamedPipeApi, dispatcher: CoroutineDispatcher = immediate()) = NamedPipeListener(
    api = api,
    instances = PipeInstances(api = api, path = PATH, securityDescriptor = SDDL, bufferSize = BUFFER),
    identity = PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS),
    first = PipeHandle(api, FIRST),
    dispatcher = dispatcher,
)

class NamedPipeListenerTest : FunSpec({
    context("accepting") {
        test("hands over the connected instance and prepares the next one") {
            val api = FakeNamedPipeApi()
            api.queueAwait(0L)

            val outcome = listenerOf(api).accept()

            val accepted = outcome.shouldBeInstanceOf<AcceptOutcome.Accepted>()

            accepted.channel.peer shouldBe IpcPeer(
                IpcUser.WindowsSid(FakeNamedPipeApi.PEER_SID),
                FakeNamedPipeApi.PEER_PROCESS,
            )
            api.created.single().isFirstInstance shouldBe false
        }

        test("takes a client that connected before the wait") {
            val api = FakeNamedPipeApi()
            api.queueAwait(-Win32Error.PIPE_CONNECTED)

            val outcome = listenerOf(api).accept()

            outcome.shouldBeInstanceOf<AcceptOutcome.Accepted>()
        }

        test("reports a failed wait and discards the instance") {
            val api = FakeNamedPipeApi()
            api.queueAwait(UNEXPECTED)

            listenerOf(api).accept() shouldBe AcceptOutcome.Refused(
                IpcAcceptResult.Failed(Win32Error.errorOf(NativeResult.fromEncoded(UNEXPECTED).errorCode)),
            )
            api.closed shouldContain FIRST
        }

        test("creates an instance when none is pending") {
            val api = FakeNamedPipeApi()
            api.queueAwait(UNEXPECTED)
            api.queueAwait(0L)
            val listener = listenerOf(api)

            listener.accept().shouldBeInstanceOf<AcceptOutcome.Refused>()

            listener.accept().shouldBeInstanceOf<AcceptOutcome.Accepted>()
        }

        test("reports that no instance could be created") {
            val api = FakeNamedPipeApi()
            api.queueAwait(UNEXPECTED)
            api.queueCreate(UNEXPECTED)
            val listener = listenerOf(api)
            listener.accept()

            listener.accept() shouldBe
                AcceptOutcome.Refused(IpcAcceptResult.Failed(Win32Error.errorOf(Win32Error.PIPE_BUSY)))
        }

        test("discards the instance when the caller gives up") {
            val api = FakeNamedPipeApi(blocker = CountDownLatch(1))

            withThreads { dispatcher -> withTimeoutOrNull(WAIT.milliseconds) { listenerOf(api, dispatcher).accept() } }

            api.cancelled shouldContain FIRST
            api.closed shouldContain FIRST
        }
    }

    context("closing") {
        test("after a failed wait has nothing left to close") {
            val api = FakeNamedPipeApi()
            api.queueAwait(UNEXPECTED)
            val listener = listenerOf(api)
            listener.accept()

            listener.close()

            api.closed shouldBe listOf(FIRST)
        }

        test("ends a pending wait and refuses to accept") {
            val api = FakeNamedPipeApi()
            val listener = listenerOf(api)

            listener.close()
            listener.close()

            api.closed shouldBe listOf(FIRST)
            listener.accept() shouldBe AcceptOutcome.Refused(IpcAcceptResult.NotListening)
        }

        test("while waiting reports that the listener stopped") {
            val api = FakeNamedPipeApi(blocker = CountDownLatch(1))

            val outcome = withThreads { dispatcher ->
                val listener = listenerOf(api, dispatcher)
                Thread { listener.close() }.start()
                listener.accept()
            }

            outcome shouldBe AcceptOutcome.Refused(IpcAcceptResult.NotListening)
        }
    }
})
