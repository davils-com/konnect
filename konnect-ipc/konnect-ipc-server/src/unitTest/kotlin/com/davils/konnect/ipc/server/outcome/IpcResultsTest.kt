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

package com.davils.konnect.ipc.server.outcome

import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.server.fixture.alicePeer
import com.davils.konnect.ipc.server.fixture.malloryPeer
import com.davils.konnect.testing.ipc.ScriptedChannel
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

private const val BUFFER: Int = 1024
private const val REASON: String = "reason"
private val broken = IpcSystemError(code = 5L, description = "broken")
private val other = IpcSystemError(code = null, description = "other")

class IpcResultsTest : FunSpec({
    context("bind results") {
        test("compare and show what they hold") {
            IpcBindResult.Unavailable(REASON) shouldBe IpcBindResult.Unavailable(REASON)
            IpcBindResult.Unavailable(REASON).hashCode() shouldBe REASON.hashCode()
            IpcBindResult.Unavailable(REASON) shouldNotBe IpcBindResult.Unavailable("other")
            IpcBindResult.Unavailable(REASON).toString() shouldBe "Unavailable(reason)"
            IpcBindResult.Failed(broken) shouldBe IpcBindResult.Failed(broken)
            IpcBindResult.Failed(broken).hashCode() shouldBe broken.hashCode()
            IpcBindResult.Failed(broken) shouldNotBe IpcBindResult.Failed(other)
            IpcBindResult.Failed(broken).toString() shouldBe "Failed($broken)"
        }
    }

    context("accept results") {
        test("compare and show what they hold") {
            val connection = IpcConnection(ScriptedChannel(alicePeer), BUFFER)
            val accepted = IpcAcceptResult.Accepted(connection)

            accepted shouldBe IpcAcceptResult.Accepted(connection)
            accepted.hashCode() shouldBe connection.hashCode()
            accepted shouldNotBe IpcAcceptResult.Accepted(IpcConnection(ScriptedChannel(alicePeer), BUFFER))
            accepted.toString() shouldStartWith "Accepted(IpcConnection("
            IpcAcceptResult.PeerRejected(alicePeer) shouldBe IpcAcceptResult.PeerRejected(alicePeer)
            IpcAcceptResult.PeerRejected(alicePeer).hashCode() shouldBe alicePeer.hashCode()
            IpcAcceptResult.PeerRejected(alicePeer) shouldNotBe IpcAcceptResult.PeerRejected(malloryPeer)
            IpcAcceptResult.PeerRejected(alicePeer).toString() shouldBe "PeerRejected($alicePeer)"
            IpcAcceptResult.Failed(broken) shouldBe IpcAcceptResult.Failed(broken)
            IpcAcceptResult.Failed(broken).hashCode() shouldBe broken.hashCode()
            IpcAcceptResult.Failed(broken) shouldNotBe IpcAcceptResult.Failed(other)
            IpcAcceptResult.Failed(broken).toString() shouldBe "Failed($broken)"
        }
    }
})
