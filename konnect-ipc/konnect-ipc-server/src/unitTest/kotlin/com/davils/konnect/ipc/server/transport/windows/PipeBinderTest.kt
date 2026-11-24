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
import com.davils.konnect.ipc.core.access.ipcAccess
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PATH: String = """\\.\pipe\helper"""
private const val SDDL: String = "D:P"
private const val BUFFER: Int = 1024
private const val UNEXPECTED: Long = -87L
private val current = IpcUser.WindowsSid(FakeNamedPipeApi.CURRENT_SID)
private val peer = IpcUser.WindowsSid(FakeNamedPipeApi.PEER_SID)

private fun binderOf(api: FakeNamedPipeApi): PipeBinder =
    PipeBinder(api, PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS), immediate())

private fun instancesOf(api: FakeNamedPipeApi): PipeInstances =
    PipeInstances(api = api, path = PATH, securityDescriptor = SDDL, bufferSize = BUFFER)

class PipeBinderTest : FunSpec({
    context("binding") {
        test("listens on the first instance") {
            val api = FakeNamedPipeApi()

            val outcome = binderOf(api).bind(instancesOf(api))

            outcome.shouldBeInstanceOf<ListenOutcome.Listening>()

            api.created.single().isFirstInstance shouldBe true
        }

        test("reports a taken name and any other error") {
            mapOf(
                -Win32Error.ACCESS_DENIED to IpcBindResult.AddressInUse,
                -Win32Error.PIPE_BUSY to IpcBindResult.AddressInUse,
                UNEXPECTED to IpcBindResult.Failed(Win32Error.errorOf(NativeResult.fromEncoded(UNEXPECTED).errorCode)),
            ).forEach { (code, result) ->
                val api = FakeNamedPipeApi()
                api.queueCreate(code)

                binderOf(api).bind(instancesOf(api)) shouldBe ListenOutcome.Refused(result)
            }
        }
    }

    context("security descriptors") {
        test("grant the server alone full access") {
            PipeSecurity.descriptor(current, IpcAccess.CURRENT_USER) shouldBe
                "D:P(A;;GA;;;${FakeNamedPipeApi.CURRENT_SID})"
        }

        test("let named users read and write and allow medium integrity") {
            val access = ipcAccess {
                allow(peer)
                allow(current)
                allow(IpcUser.UnixUser.ROOT)
            }

            PipeSecurity.descriptor(current, access) shouldBe
                "D:P(A;;GA;;;${FakeNamedPipeApi.CURRENT_SID})(A;;GRGW;;;${FakeNamedPipeApi.PEER_SID})S:(ML;;NW;;;ME)"
        }
    }
})
