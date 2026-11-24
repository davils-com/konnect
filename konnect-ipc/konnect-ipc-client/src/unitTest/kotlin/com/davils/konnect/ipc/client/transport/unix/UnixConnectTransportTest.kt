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

package com.davils.konnect.ipc.client.transport.unix

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.fixture.HELPER
import com.davils.konnect.ipc.client.fixture.HELPER_SOCKET
import com.davils.konnect.ipc.client.fixture.immediate
import com.davils.konnect.ipc.client.fixture.refusal
import com.davils.konnect.ipc.client.fixture.withThreads
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.testing.ipc.FakeIpcEnvironment
import com.davils.konnect.testing.ipc.FakeUnixSocket
import com.davils.konnect.testing.ipc.FakeUnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val BUFFER: Int = 1024
private const val SLOW: Long = 5_000L
private const val WAIT: Int = 50
private const val BROKEN: String = "broken /secret/path"
private val pipe = IpcAddress.WindowsPipe("discord-ipc-0")

private fun transportOf(
    api: FakeUnixSocketApi,
    dispatcher: CoroutineDispatcher = immediate(),
    environment: FakeIpcEnvironment = FakeIpcEnvironment(),
): UnixConnectTransport =
    UnixConnectTransport(api = api, environment = environment, isMacOs = false, dispatcher = dispatcher)

private suspend fun UnixConnectTransport.refusalOf(address: IpcAddress = HELPER): IpcConnectResult {
    val outcome = connect(address, 1.seconds, BUFFER)
    return outcome.refusal()
}

class UnixConnectTransportTest : FunSpec({
    context("the current user") {
        test("is the user the process runs as") {
            transportOf(FakeUnixSocketApi()).currentUser shouldBe IpcUser.UnixUser("alice")
        }

        test("is unknown when the name of the user is no user name") {
            val environment = FakeIpcEnvironment(userName = "")

            transportOf(FakeUnixSocketApi(), environment = environment).currentUser.shouldBeNull()
        }
    }

    context("connecting") {
        test("opens an existing socket") {
            val outcome = transportOf(
                FakeUnixSocketApi(existing = setOf(HELPER_SOCKET)),
            ).connect(HELPER, 1.seconds, BUFFER)

            val channel = outcome.shouldBeInstanceOf<ConnectOutcome.Connected>().channel
            channel.peer.user shouldBe IpcUser.UnixUser("alice")
        }

        test("does not find a missing or silent socket") {
            val silent = FakeUnixSocketApi(
                existing = setOf(HELPER_SOCKET),
                connecting = { throw ConnectException("no") },
            )

            transportOf(FakeUnixSocketApi()).refusalOf() shouldBe IpcConnectResult.NotFound
            transportOf(silent).refusalOf() shouldBe IpcConnectResult.NotFound
        }

        test("is denied a socket it may not write to") {
            val api = FakeUnixSocketApi(existing = setOf(HELPER_SOCKET), isWritable = false)

            transportOf(api).refusalOf() shouldBe IpcConnectResult.AccessDenied
        }

        test("reports any other problem without the message of the jdk") {
            val api = FakeUnixSocketApi(existing = setOf(HELPER_SOCKET), connecting = { throw IOException(BROKEN) })

            transportOf(api).refusalOf() shouldBe
                IpcConnectResult.Failed(IpcSystemError(code = null, description = "IOException"))
        }

        test("gives up after its timeout") {
            val api = FakeUnixSocketApi(
                existing = setOf(HELPER_SOCKET),
                connecting = {
                    CountDownLatch(1).await(SLOW, TimeUnit.MILLISECONDS)
                    FakeUnixSocket()
                },
            )

            val outcome = withThreads { dispatcher ->
                transportOf(api, dispatcher).connect(HELPER, WAIT.milliseconds, BUFFER)
            }

            outcome.refusal() shouldBe IpcConnectResult.TimedOut
        }

        test("cannot reach a windows pipe") {
            transportOf(FakeUnixSocketApi()).refusalOf(pipe) shouldBe IpcConnectResult.Unavailable(
                "pipe:discord-ipc-0 cannot be reached through a Unix domain socket",
            )
        }
    }
})
