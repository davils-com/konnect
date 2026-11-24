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

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.fixture.HELPER
import com.davils.konnect.ipc.core.fixture.SOCKET_PATH
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith

private const val STRANGER: Long = 999L

class PipeSessionTest : FunSpec({
    context("opening a session") {
        test("names the current user") {
            val api = FakeNamedPipeApi()

            val session = PipeSession.open(NativeLibrary { api }, FakeNamedPipeApi.CURRENT_PROCESS)

            session.shouldNotBeNull().user shouldBe IpcUser.WindowsSid(FakeNamedPipeApi.CURRENT_SID)
            session.api shouldBe api
        }

        test("fails without the library or the user") {
            val missing = NativeLibrary<NamedPipeApi> { throw UnsatisfiedLinkError("missing") }
            val stranger = NativeLibrary { FakeNamedPipeApi() }

            PipeSession.open(missing, FakeNamedPipeApi.CURRENT_PROCESS).shouldBeNull()
            PipeSession.open(stranger, STRANGER).shouldBeNull()
        }
    }

    context("the target of an address") {
        test("is the pipe of the address or nothing") {
            val library = NativeLibrary { FakeNamedPipeApi() }
            val session = PipeSession.open(library, FakeNamedPipeApi.CURRENT_PROCESS).shouldNotBeNull()

            val target = session.targetOf(HELPER).shouldNotBeNull()

            target.path shouldEndWith "helper"
            target.session shouldBe session
            session.targetOf(IpcAddress.UnixSocket(SOCKET_PATH)).shouldBeNull()
        }

        test("is equal to a target of the same session and path") {
            val session = PipeSession
                .open(NativeLibrary { FakeNamedPipeApi() }, FakeNamedPipeApi.CURRENT_PROCESS)
                .shouldNotBeNull()
            val target = session.targetOf(HELPER).shouldNotBeNull()

            target shouldBe PipeTarget(session, target.path)
            target.hashCode() shouldBe PipeTarget(session, target.path).hashCode()
            target shouldNotBe PipeTarget(session, "other")
            target shouldNotBe PipeTarget(PipeSession(session.api, session.identity, session.user), target.path)
            target.equals(target.path) shouldBe false
            target.toString() shouldBe "PipeTarget(path=${target.path})"
        }
    }
})
