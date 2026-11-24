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

package com.davils.konnect.ipc.client

import com.davils.konnect.ipc.client.fixture.alicePeer
import com.davils.konnect.ipc.client.fixture.malloryPeer
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val broken: IpcSystemError = IpcSystemError(code = 1L, description = "Win32 error 1")

class IpcConnectResultTest : FunSpec({
    context("a result") {
        test("is equal to a result of the same kind and value") {
            val connection = TestIpc.connections().client

            IpcConnectResult.Connected(connection) shouldBe IpcConnectResult.Connected(connection)
            IpcConnectResult.ServerRejected(alicePeer) shouldBe IpcConnectResult.ServerRejected(alicePeer)
            IpcConnectResult.Unavailable("why") shouldBe IpcConnectResult.Unavailable("why")
            IpcConnectResult.Failed(broken) shouldBe IpcConnectResult.Failed(broken)
            IpcConnectResult.Connected(connection).hashCode() shouldBe connection.hashCode()
            IpcConnectResult.ServerRejected(alicePeer).hashCode() shouldBe alicePeer.hashCode()
            IpcConnectResult.Unavailable("why").hashCode() shouldBe "why".hashCode()
            IpcConnectResult.Failed(broken).hashCode() shouldBe broken.hashCode()
        }

        test("differs from a result of another value or kind") {
            val connection = TestIpc.connections().client
            val other = TestIpc.connections().client

            IpcConnectResult.Connected(connection) shouldNotBe IpcConnectResult.Connected(other)
            IpcConnectResult.ServerRejected(alicePeer) shouldNotBe IpcConnectResult.ServerRejected(malloryPeer)
            IpcConnectResult.Unavailable("why") shouldNotBe IpcConnectResult.Unavailable("how")
            IpcConnectResult.Failed(broken) shouldNotBe IpcConnectResult.NotFound
            IpcConnectResult.Connected(connection).equals(connection) shouldBe false
            IpcConnectResult.ServerRejected(alicePeer).equals(alicePeer) shouldBe false
            IpcConnectResult.Unavailable("why").equals("why") shouldBe false
            IpcConnectResult.Failed(broken).equals(broken) shouldBe false
        }

        test("shows its value") {
            val connection = TestIpc.connections().client

            IpcConnectResult.Connected(connection).toString() shouldBe "Connected($connection)"
            IpcConnectResult.ServerRejected(alicePeer).toString() shouldBe "ServerRejected($alicePeer)"
            IpcConnectResult.Unavailable("why").toString() shouldBe "Unavailable(why)"
            IpcConnectResult.Failed(broken).toString() shouldBe "Failed($broken)"
            IpcConnectResult.TimedOut.toString() shouldBe "TimedOut"
        }
    }

    context("the outcome of a result") {
        test("names every kind in snake case") {
            val connection = TestIpc.connections().client

            listOf(
                IpcConnectResult.Connected(connection),
                IpcConnectResult.NotFound,
                IpcConnectResult.AccessDenied,
                IpcConnectResult.TimedOut,
                IpcConnectResult.ServerRejected(alicePeer),
                IpcConnectResult.Unavailable("why"),
                IpcConnectResult.Failed(broken),
            ).map { result -> result.outcome } shouldBe listOf(
                "connected",
                "not_found",
                "access_denied",
                "timed_out",
                "server_rejected",
                "unavailable",
                "failed",
            )
        }
    }

    context("codes") {
        test("keep their stable values") {
            IpcClientViolationCodes.INVALID_CONNECT_TIMEOUT.value shouldBe "konnect.ipc.client.invalid-connect-timeout"
        }
    }
})
