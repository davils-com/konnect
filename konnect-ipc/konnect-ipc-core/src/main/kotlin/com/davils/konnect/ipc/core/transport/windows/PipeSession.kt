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

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeLibrary

/**
 * The named pipes of this process, with the user it runs as.
 *
 * @property api the pipe operations.
 * @property identity reads who is on the other side of a pipe.
 * @property user the user this process runs as.
 * @since 1.0.0
 */
@DavilsInternal
public class PipeSession(
    public val api: NamedPipeApi,
    public val identity: PipeIdentity,
    public val user: IpcUser.WindowsSid,
) {
    /**
     * Finds the pipe [address] leads to.
     *
     * @param address the address.
     * @return the target, or `null` for a Unix socket.
     * @since 1.0.0
     */
    public fun targetOf(address: IpcAddress): PipeTarget? {
        val path = PipeNames.of(address, user) ?: return null
        return PipeTarget(this, path)
    }

    /**
     * Opens sessions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Opens the session of this process.
         *
         * @param library the native library of the pipes.
         * @param currentProcessId the identifier of this process.
         * @return the session, or `null` if the library cannot be loaded or the user of this process cannot be read.
         * @since 1.0.0
         */
        public fun open(library: NativeLibrary<NamedPipeApi>, currentProcessId: Long): PipeSession? {
            val api = library.api ?: return null
            val identity = PipeIdentity(api, currentProcessId)
            val user = identity.currentUser()
            return user?.let { current -> PipeSession(api, identity, current) }
        }
    }
}
