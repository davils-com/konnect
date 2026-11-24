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

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.fixture.refusal
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary
import com.davils.kore.core.platform.NativeLoader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Path

private const val BUFFER: Int = 1024
private val socket = IpcAddress.UnixSocket(Path.of("/run/konnect.sock"))

private fun transportOf(loader: NativeLoader<NamedPipeApi>): NamedPipeListenTransport = NamedPipeListenTransport(
    library = NativeLibrary(loader),
    dispatcher = immediate(),
    currentProcessId = FakeNamedPipeApi.CURRENT_PROCESS,
)

class NamedPipeListenTransportTest : FunSpec({
    context("with the native library") {
        test("knows the current user") {
            transportOf { FakeNamedPipeApi() }.currentUser shouldBe IpcUser.WindowsSid(FakeNamedPipeApi.CURRENT_SID)
        }

        test("listens on a pipe only the current user may open") {
            val api = FakeNamedPipeApi()

            val listened = transportOf { api }.listen(HELPER, IpcAccess.CURRENT_USER, BUFFER)

            listened.shouldBeInstanceOf<ListenOutcome.Listening>()
            api.created.single().securityDescriptor shouldBe "D:P(A;;GA;;;${FakeNamedPipeApi.CURRENT_SID})"
            api.created.single().path shouldContain FakeNamedPipeApi.CURRENT_SID
            api.created.single().isFirstInstance shouldBe true
        }

        test("cannot reach a unix socket") {
            val listened = transportOf { FakeNamedPipeApi() }.listen(socket, IpcAccess.CURRENT_USER, BUFFER)

            listened.refusal() shouldBe
                IpcBindResult.Unavailable("the address cannot be reached through a Windows named pipe")
        }
    }

    context("without the native library") {
        test("is unavailable") {
            val transport = transportOf { throw UnsatisfiedLinkError("missing") }

            val listened = transport.listen(HELPER, IpcAccess.CURRENT_USER, BUFFER)

            transport.currentUser.shouldBeNull()
            listened.refusal() shouldBe IpcBindResult.Unavailable(
                "the native named pipe library or the identity of the current user is missing",
            )
        }
    }
})
