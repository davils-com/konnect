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

package com.davils.konnect.ipc.server.transport.unix

import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.server.fixture.HELPER_DIRECTORY
import com.davils.konnect.ipc.server.fixture.HELPER_SOCKET
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.testing.ipc.FakeUnixServerSocket
import com.davils.konnect.testing.ipc.FakeUnixSocket
import com.davils.konnect.testing.ipc.FakeUnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

private const val WAIT: Int = 50
private val staging: Path = HELPER_DIRECTORY.resolve(".konnect-0badc0de")

private fun listenerOf(server: UnixServerSocket, api: FakeUnixSocketApi = FakeUnixSocketApi()): UnixListener =
    UnixListener(server = server, path = HELPER_SOCKET, staging = staging, api = api, dispatcher = immediate())

class UnixListenerTest : FunSpec({
    context("accepting") {
        test("hands over the next socket and skips wakeups without one") {
            val listener = listenerOf(FakeUnixServerSocket(listOf(null, FakeUnixSocket())))

            listener.accept().shouldBeInstanceOf<AcceptOutcome.Accepted>()
        }

        test("reports a closed listener") {
            listOf(ClosedChannelException(), ClosedSelectorException()).forEach { failure ->
                val server = object : UnixServerSocket {
                    override fun accept() = throw failure

                    override fun wakeup() = Unit

                    override fun close() = Unit
                }
                listenerOf(server).accept() shouldBe
                    AcceptOutcome.Refused(IpcAcceptResult.NotListening)
            }
        }

        test("reports any other problem without its message") {
            listenerOf(FakeUnixServerSocket(failure = IOException("broken"))).accept() shouldBe
                AcceptOutcome.Refused(IpcAcceptResult.Failed(IpcSystemError(code = null, description = "IOException")))
        }

        test("wakes the server when the caller gives up") {
            val server = FakeUnixServerSocket()

            withThreads { dispatcher ->
                val listener = UnixListener(
                    server = server,
                    path = HELPER_SOCKET,
                    staging = staging,
                    api = FakeUnixSocketApi(),
                    dispatcher = dispatcher,
                )
                withTimeoutOrNull(WAIT.milliseconds) { listener.accept() }
            }

            server.wakeups shouldBe 1
        }
    }

    context("closing") {
        test("closes the server once, removes the socket and its staging directory and refuses to accept") {
            val server = FakeUnixServerSocket(listOf(FakeUnixSocket()))
            val api = FakeUnixSocketApi(existing = setOf(HELPER_SOCKET))
            val listener = listenerOf(server, api)

            listener.close()
            listener.close()

            server.closes shouldBe 1
            api.deleted shouldBe listOf(HELPER_SOCKET, staging)
            listener.accept() shouldBe AcceptOutcome.Refused(IpcAcceptResult.NotListening)
        }
    }
})
