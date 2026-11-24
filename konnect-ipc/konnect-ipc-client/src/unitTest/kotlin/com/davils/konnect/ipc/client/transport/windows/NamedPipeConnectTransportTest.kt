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

package com.davils.konnect.ipc.client.transport.windows

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.fixture.HELPER
import com.davils.konnect.ipc.client.fixture.SOCKET
import com.davils.konnect.ipc.client.fixture.immediate
import com.davils.konnect.ipc.client.fixture.refusal
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

private const val BUFFER: Int = 1024
private val unreachable: String = "$SOCKET cannot be reached through a Windows named pipe"
private const val MISSING: String = "the native named pipe library or the identity of the current user is missing"

private fun transportOf(library: NativeLibrary<NamedPipeApi>): NamedPipeConnectTransport = NamedPipeConnectTransport(
    library = library,
    dispatcher = immediate(),
    currentProcessId = FakeNamedPipeApi.CURRENT_PROCESS,
)

class NamedPipeConnectTransportTest : FunSpec({
    context("with the native library") {
        test("knows the current user and connects to a pipe that names the server") {
            val transport = transportOf(NativeLibrary { FakeNamedPipeApi() })

            val connected = transport.connect(HELPER, 1.seconds, BUFFER)

            transport.currentUser shouldBe IpcUser.WindowsSid(FakeNamedPipeApi.CURRENT_SID)
            val channel = connected.shouldBeInstanceOf<ConnectOutcome.Connected>().channel
            channel.peer.user shouldBe IpcUser.WindowsSid(FakeNamedPipeApi.PEER_SID)
        }

        test("cannot reach a unix socket") {
            val connected = transportOf(NativeLibrary { FakeNamedPipeApi() }).connect(SOCKET, 1.seconds, BUFFER)

            connected.refusal() shouldBe IpcConnectResult.Unavailable(unreachable)
        }
    }

    context("without the native library") {
        test("is unavailable") {
            val transport = transportOf(NativeLibrary { throw UnsatisfiedLinkError("missing") })

            val connected = transport.connect(HELPER, 1.seconds, BUFFER)

            transport.currentUser.shouldBeNull()
            connected.refusal() shouldBe IpcConnectResult.Unavailable(MISSING)
        }
    }
})
