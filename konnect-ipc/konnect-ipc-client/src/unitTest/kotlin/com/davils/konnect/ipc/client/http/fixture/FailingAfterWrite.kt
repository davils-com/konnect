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

package com.davils.konnect.ipc.client.http.fixture

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.konnect.ipc.core.transport.IpcChannelRead
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CompletableDeferred

private val failing = IpcSystemError(code = 1, description = "broken")

internal class FailingAfterWrite : IpcChannel {
    private val written = CompletableDeferred<Unit>()

    override val peer: IpcPeer = TestIpc.SERVER

    override suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead {
        written.await()
        return IpcChannelRead.Failed(failing)
    }

    override suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult {
        written.complete(Unit)
        return IpcWriteResult.Written
    }

    override suspend fun closeOutput(): IpcWriteResult = IpcWriteResult.Written

    override fun close() {
        written.complete(Unit)
    }
}
