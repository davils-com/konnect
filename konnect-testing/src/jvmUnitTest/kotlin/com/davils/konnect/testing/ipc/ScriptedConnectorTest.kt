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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.client.IpcConnectResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScriptedConnectorTest : FunSpec({
    context("connecting") {
        test("answers with the scripted results in order and counts every attempt") {
            val connection = TestIpc.connections().client
            val connector = ScriptedConnector(
                listOf(IpcConnectResult.AccessDenied, IpcConnectResult.Connected(connection)),
            )

            connector.connect() shouldBe IpcConnectResult.AccessDenied
            connector.connect() shouldBe IpcConnectResult.Connected(connection)
            connector.attempts shouldBe 2
        }

        test("answers with the exhausted result once the script has run out") {
            val connector = ScriptedConnector(emptyList(), exhausted = IpcConnectResult.TimedOut)

            connector.connect() shouldBe IpcConnectResult.TimedOut
            connector.connect() shouldBe IpcConnectResult.TimedOut
        }

        test("answers that nobody listens once the script has run out by default") {
            ScriptedConnector(emptyList()).connect() shouldBe IpcConnectResult.NotFound
        }

        test("keeps its script when the list it was given changes") {
            val results = mutableListOf<IpcConnectResult>(IpcConnectResult.AccessDenied)
            val connector = ScriptedConnector(results)
            results[0] = IpcConnectResult.TimedOut

            connector.connect() shouldBe IpcConnectResult.AccessDenied
        }
    }

    context("a scripted connector") {
        test("shows its script and its attempts") {
            val connector = ScriptedConnector(listOf(IpcConnectResult.NotFound))
            connector.connect()

            connector.toString() shouldBe "ScriptedConnector(results=1, attempts=1)"
        }
    }
})
