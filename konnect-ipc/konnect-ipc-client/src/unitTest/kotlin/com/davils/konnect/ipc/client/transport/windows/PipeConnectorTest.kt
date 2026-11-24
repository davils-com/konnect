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
import com.davils.konnect.ipc.client.fixture.immediate
import com.davils.konnect.ipc.client.fixture.refusal
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.core.transport.windows.Win32Error
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val PATH: String = """\\.\pipe\helper"""
private const val BUFFER: Int = 1024
private const val UNEXPECTED: Long = 87L
private const val BUSY: Long = -Win32Error.PIPE_BUSY

private fun later(): ComparableTimeMark = TimeSource.Monotonic.markNow() + 1.seconds

private suspend fun connect(api: FakeNamedPipeApi, deadline: ComparableTimeMark = later()): ConnectOutcome {
    val connector = PipeConnector(api, PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS), immediate())
    return connector.connect(PATH, deadline, BUFFER)
}

class PipeConnectorTest : FunSpec({
    context("connecting") {
        test("opens the pipe") {
            connect(FakeNamedPipeApi()).shouldBeInstanceOf<ConnectOutcome.Connected>()
        }

        test("reports a missing pipe, a denied pipe and any other error") {
            mapOf(
                -Win32Error.FILE_NOT_FOUND to IpcConnectResult.NotFound,
                -Win32Error.ACCESS_DENIED to IpcConnectResult.AccessDenied,
                -UNEXPECTED to IpcConnectResult.Failed(Win32Error.errorOf(UNEXPECTED)),
            ).forEach { (code, result) ->
                val api = FakeNamedPipeApi()
                api.queueOpen(code)

                connect(api).refusal() shouldBe result
            }
        }
    }

    context("a busy pipe") {
        test("is waited for and opened again") {
            val api = FakeNamedPipeApi()
            api.queueOpen(BUSY)
            api.queueOpen(BUSY)

            connect(api).shouldBeInstanceOf<ConnectOutcome.Connected>()
        }

        test("times out when the wait or the deadline runs out") {
            val api = FakeNamedPipeApi()
            api.queueOpen(BUSY)
            api.queueWait(-Win32Error.SEM_TIMEOUT)
            val expired = FakeNamedPipeApi()
            expired.queueOpen(BUSY)

            connect(api).refusal() shouldBe IpcConnectResult.TimedOut
            connect(expired, TimeSource.Monotonic.markNow()).refusal() shouldBe IpcConnectResult.TimedOut
        }

        test("is gone when it disappears while waiting") {
            val api = FakeNamedPipeApi()
            api.queueOpen(BUSY)
            api.queueWait(-Win32Error.FILE_NOT_FOUND)

            connect(api).refusal() shouldBe IpcConnectResult.NotFound
        }

        test("reports any other failure of the wait") {
            val api = FakeNamedPipeApi()
            api.queueOpen(BUSY)
            api.queueWait(-UNEXPECTED)

            connect(api).refusal() shouldBe IpcConnectResult.Failed(Win32Error.errorOf(UNEXPECTED))
        }
    }
})
