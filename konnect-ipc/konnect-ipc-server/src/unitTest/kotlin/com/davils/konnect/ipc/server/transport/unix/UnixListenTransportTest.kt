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

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.fixture.refusal
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.FakeIpcEnvironment
import com.davils.konnect.testing.ipc.FakeUnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val BUFFER: Int = 1024
private val pipe = IpcAddress.WindowsPipe("discord-ipc-0")

private fun transportOf(api: FakeUnixSocketApi, user: String = FakeIpcEnvironment.USER): UnixListenTransport =
    UnixListenTransport(
        api = api,
        environment = FakeIpcEnvironment(userName = user),
        isMacOs = false,
        dispatcher = immediate(),
    )

class UnixListenTransportTest : FunSpec({
    context("the current user") {
        test("is the user the process runs as") {
            transportOf(FakeUnixSocketApi()).currentUser shouldBe IpcUser.UnixUser("alice")
        }

        test("is unknown when the name of the user is no valid name") {
            transportOf(FakeUnixSocketApi(), user = "").currentUser.shouldBeNull()
        }
    }

    context("listening") {
        test("binds a named address") {
            val transport = transportOf(FakeUnixSocketApi())

            transport.listen(HELPER, IpcAccess.CURRENT_USER, BUFFER).shouldBeInstanceOf<ListenOutcome.Listening>()
        }

        test("cannot reach a windows pipe") {
            val outcome = transportOf(FakeUnixSocketApi()).listen(pipe, IpcAccess.CURRENT_USER, BUFFER)

            outcome.refusal().shouldBeInstanceOf<IpcBindResult.Unavailable>()
        }
    }
})
