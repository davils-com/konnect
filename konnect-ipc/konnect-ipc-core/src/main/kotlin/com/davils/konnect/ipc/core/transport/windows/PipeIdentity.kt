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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult
import java.nio.ByteBuffer

private const val BYTES_PER_CHARACTER: Int = 2

/**
 * Reads who runs a process on the other side of a pipe, and who runs this process.
 *
 * A user whose security identifier cannot be read, does not fit [Limits.MAX_SID_LENGTH] or is not a security
 * identifier is unknown.
 *
 * @param api the identity operations of the pipes.
 * @param currentProcessId the identifier of this process.
 * @since 1.0.0
 */
@DavilsInternal
public class PipeIdentity(private val api: PipeIdentityApi, private val currentProcessId: Long) {
    /**
     * Reads the user this process runs as.
     *
     * @return the user, or `null` if it cannot be read.
     * @since 1.0.0
     */
    public fun currentUser(): IpcUser.WindowsSid? = sidOf(currentProcessId)

    /**
     * Reads the client of the server pipe [handle].
     *
     * @param handle the server end of a connected pipe.
     * @return the client, or [IpcPeer.UNKNOWN] if its process cannot be read.
     * @since 1.0.0
     */
    public fun client(handle: Long): IpcPeer = peerOf(api.clientProcessId(handle))

    /**
     * Reads the server of the client pipe [handle].
     *
     * @param handle the client end of a connected pipe.
     * @return the server, or [IpcPeer.UNKNOWN] if its process cannot be read.
     * @since 1.0.0
     */
    public fun server(handle: Long): IpcPeer = peerOf(api.serverProcessId(handle))

    private fun peerOf(result: NativeResult): IpcPeer {
        if (!result.isSuccess) return IpcPeer.UNKNOWN
        val processId = result.value
        return IpcPeer(user = sidOf(processId), processId = processId)
    }

    private fun sidOf(processId: Long): IpcUser.WindowsSid? {
        val buffer = ByteBuffer.allocateDirect(Limits.MAX_SID_LENGTH * BYTES_PER_CHARACTER)
        val length = api.processUserSid(processId, buffer)
        val isReadable = length.isSuccess && length.value in 1..Limits.MAX_SID_LENGTH
        if (!isReadable) return null
        val bytes = ByteArray(length.value.toInt() * BYTES_PER_CHARACTER)
        buffer.get(0, bytes)
        val text = String(bytes, Charsets.UTF_16LE)
        return text.takeIf(IpcUser.WindowsSid::isValid)?.let(IpcUser::WindowsSid)
    }
}
