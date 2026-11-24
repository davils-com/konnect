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

package com.davils.konnect.ipc.client

import com.davils.konnect.ipc.client.fixture.isWindows
import com.davils.konnect.ipc.client.fixture.withThreads
import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcName
import com.davils.konnect.ipc.core.transport.windows.PipeSession
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.Uuid

private const val SUFFIX: Int = 12
private const val BUFFER: Int = 4096

private fun uniqueAddress(): IpcAddress.Named {
    val suffix = Uuid.random().toHexString()
    return IpcAddress.Named(IpcName("konnect-test-${suffix.take(SUFFIX)}"))
}

class NamedPipeClientTest : FunSpec({
    context("a client of a windows named pipe").config(enabledIf = { isWindows }) {
        test("connects to a listening pipe and names the server") {
            val session = PipeSession.open(IpcPlatform.namedPipes, IpcPlatform.currentProcessId).shouldNotBeNull()
            val address = uniqueAddress()
            val path = session.targetOf(address).shouldNotBeNull().path
            val instance = session.api.createServer(
                path = path,
                securityDescriptor = "D:P(A;;GA;;;${session.user.value})",
                bufferSize = BUFFER,
                isFirstInstance = true,
            )

            withThreads { dispatcher ->
                val result = ipcClient(dispatcher) { this.address = address }.connect()
                val connection = result.shouldBeInstanceOf<IpcConnectResult.Connected>().connection

                connection.peer.user shouldBe session.user
                connection.close()
            }
            session.api.close(instance.value)
        }

        test("finds no pipe where nobody listens") {
            withThreads { dispatcher ->
                ipcClient(dispatcher) { this.address = uniqueAddress() }.connect() shouldBe IpcConnectResult.NotFound
            }
        }
    }
})
