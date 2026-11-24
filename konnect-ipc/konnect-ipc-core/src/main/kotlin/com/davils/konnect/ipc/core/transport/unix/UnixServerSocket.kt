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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.kore.core.annotation.DavilsInternal

/**
 * A bound Unix domain socket that accepts connections.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface UnixServerSocket {
    /**
     * Waits for the next connection, blocking the calling thread until one arrived or [wakeup] was called.
     *
     * @return the connected socket, or `null` if the wait was woken up without a connection.
     * @since 1.0.0
     */
    public fun accept(): UnixSocket?

    /**
     * Ends a pending [accept]; may be called from any thread.
     *
     * @since 1.0.0
     */
    public fun wakeup()

    /**
     * Closes the socket; its path stays on the file system.
     *
     * @since 1.0.0
     */
    public fun close()
}
