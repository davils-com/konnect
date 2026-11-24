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

package com.davils.konnect.ipc.server

import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.fixture.alicePeer
import com.davils.konnect.ipc.server.fixture.malloryPeer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private fun equalityOf(event: IpcServerEvent, twin: IpcServerEvent, other: IpcServerEvent) {
    event shouldBe twin
    event.hashCode() shouldBe twin.hashCode()
    event shouldNotBe other
    event shouldNotBe HELPER
    event.address shouldBe HELPER
}

class IpcServerEventTest : FunSpec({
    context("events") {
        test("are equal when they say the same") {
            equalityOf(
                event = IpcServerEvent.Bound(HELPER),
                twin = IpcServerEvent.Bound(HELPER),
                other = IpcServerEvent.Bound(SERVICE),
            )
            equalityOf(
                event = IpcServerEvent.Closed(HELPER),
                twin = IpcServerEvent.Closed(HELPER),
                other = IpcServerEvent.Closed(SERVICE),
            )
            equalityOf(
                event = IpcServerEvent.Accepted(HELPER, alicePeer),
                twin = IpcServerEvent.Accepted(HELPER, alicePeer),
                other = IpcServerEvent.Accepted(SERVICE, alicePeer),
            )
            equalityOf(
                event = IpcServerEvent.PeerRejected(HELPER, alicePeer),
                twin = IpcServerEvent.PeerRejected(HELPER, alicePeer),
                other = IpcServerEvent.PeerRejected(SERVICE, alicePeer),
            )
        }

        test("show what happened and where") {
            val rejected = IpcServerEvent.PeerRejected(HELPER, malloryPeer)

            IpcServerEvent.Bound(HELPER).toString() shouldBe "Bound(user:helper)"
            IpcServerEvent.Closed(HELPER).toString() shouldBe "Closed(user:helper)"
            IpcServerEvent.Accepted(HELPER, alicePeer).toString() shouldBe "Accepted(user:helper, $alicePeer)"
            rejected.toString() shouldBe "PeerRejected(user:helper, $malloryPeer)"
        }

        test("differ between accepted and refused peers") {
            IpcServerEvent.Accepted(HELPER, alicePeer) shouldNotBe IpcServerEvent.PeerRejected(HELPER, alicePeer)
            IpcServerEvent.Accepted(HELPER, alicePeer) shouldNotBe IpcServerEvent.Accepted(HELPER, malloryPeer)
            IpcServerEvent.PeerRejected(HELPER, alicePeer) shouldNotBe IpcServerEvent.PeerRejected(HELPER, malloryPeer)
        }
    }
})
