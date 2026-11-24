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
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcName
import com.davils.konnect.ipc.core.address.IpcScope
import com.davils.konnect.ipc.core.fixture.HELPER
import com.davils.konnect.ipc.core.fixture.HELPER_NAME
import com.davils.konnect.ipc.core.fixture.SOCKET_PATH
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer

private const val HANDLE: Long = 7L
private const val UNKNOWN_PROCESS: Long = 300L
private const val TOO_LONG_PROCESS: Long = 301L
private val current = IpcUser.WindowsSid(FakeNamedPipeApi.CURRENT_SID)
private val peer = IpcUser.WindowsSid(FakeNamedPipeApi.PEER_SID)

class PipeBasicsTest : FunSpec({
    context("win32 errors") {
        test("carry their code and describe it without anything else") {
            Win32Error.errorOf(Win32Error.ACCESS_DENIED).code shouldBe Win32Error.ACCESS_DENIED
            Win32Error.errorOf(Win32Error.ACCESS_DENIED).description shouldBe "Win32 error 5"
            Win32Error.endOfStream shouldBe setOf(
                Win32Error.BROKEN_PIPE,
                Win32Error.NO_DATA,
                Win32Error.PIPE_NOT_CONNECTED,
                Win32Error.OPERATION_ABORTED,
            )
        }
    }

    context("pipe names") {
        test("of a user live below the namespace of konnect and the user") {
            PipeNames.of(
                HELPER,
                current,
            ) shouldBe """\\.\pipe\com.davils.konnect\${FakeNamedPipeApi.CURRENT_SID}\helper"""
        }

        test("of the system live below the namespace of konnect") {
            val system = IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.System)

            PipeNames.of(system, current) shouldBe """\\.\pipe\com.davils.konnect\helper"""
        }

        test("of other programs are taken as they are") {
            PipeNames.of(IpcAddress.WindowsPipe("discord-ipc-0"), current) shouldBe """\\.\pipe\discord-ipc-0"""
            PipeNames.of(IpcAddress.UnixSocket(SOCKET_PATH), current).shouldBeNull()
        }
    }

    context("identities") {
        test("name the user of a process") {
            val identity = PipeIdentity(FakeNamedPipeApi(), FakeNamedPipeApi.CURRENT_PROCESS)

            identity.currentUser() shouldBe current
            identity.client(HANDLE) shouldBe IpcPeer(peer, FakeNamedPipeApi.PEER_PROCESS)
            identity.server(HANDLE) shouldBe IpcPeer(peer, FakeNamedPipeApi.PEER_PROCESS)
        }

        test("are unknown when the process or its user cannot be read") {
            val api = FakeNamedPipeApi(sids = mapOf(UNKNOWN_PROCESS to "not a sid"))
            api.peerProcessId = -Win32Error.ACCESS_DENIED

            val unknown = PipeIdentity(api, UNKNOWN_PROCESS)
            val stranger = PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS)

            unknown.currentUser().shouldBeNull()
            stranger.currentUser().shouldBeNull()
            stranger.client(HANDLE) shouldBe IpcPeer.UNKNOWN
        }

        test("are unknown when the user of a process is empty or does not fit the buffer") {
            val api = FakeNamedPipeApi(
                sids = mapOf(
                    FakeNamedPipeApi.CURRENT_PROCESS to "",
                    TOO_LONG_PROCESS to "S" + "-1".repeat(Limits.MAX_SID_LENGTH),
                ),
            )

            val empty = PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS)
            val tooLong = PipeIdentity(api, TOO_LONG_PROCESS)

            empty.currentUser().shouldBeNull()
            tooLong.currentUser().shouldBeNull()
        }

        test("are unknown when the library reports more characters than the limit") {
            val lying = object : PipeIdentityApi by FakeNamedPipeApi() {
                override fun processUserSid(processId: Long, buffer: ByteBuffer): NativeResult =
                    NativeResult.success(Limits.MAX_SID_LENGTH + 1L)
            }

            val identity = PipeIdentity(lying, FakeNamedPipeApi.CURRENT_PROCESS)

            identity.currentUser().shouldBeNull()
        }
    }
})
