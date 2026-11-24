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

package com.davils.konnect.ipc.server

import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.core.transport.windows.PipeSession
import com.davils.konnect.ipc.server.fixture.isWindows
import com.davils.konnect.ipc.server.fixture.uniqueAddress
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class NamedPipeServerTest : FunSpec({
    context("a server on a windows named pipe").config(enabledIf = { isWindows }) {
        test("refuses a second server on the same name") {
            withThreads { dispatcher ->
                val address = uniqueAddress()
                val first = ipcServer(dispatcher) { this.address = address }

                first.bind() shouldBe IpcBindResult.Bound
                ipcServer(dispatcher) { this.address = address }.bind() shouldBe IpcBindResult.AddressInUse
                first.close()
            }
        }

        test("accepts a client and names it by its security identifier and process") {
            val session = PipeSession.open(IpcPlatform.namedPipes, IpcPlatform.currentProcessId).shouldNotBeNull()

            withThreads { dispatcher ->
                val address = uniqueAddress()
                val server = ipcServer(dispatcher) { this.address = address }
                server.bind()
                coroutineScope {
                    val accepting = async { server.accept() }
                    val handle = session.api.open(session.targetOf(address).shouldNotBeNull().path)
                    val accepted = accepting.await().shouldBeInstanceOf<IpcAcceptResult.Accepted>()

                    handle.isSuccess shouldBe true
                    accepted.connection.peer.user shouldBe session.user
                    accepted.connection.peer.processId shouldBe IpcPlatform.currentProcessId
                    session.api.close(handle.value)
                    accepted.connection.close()
                }
                server.close()
            }
        }
    }
})
