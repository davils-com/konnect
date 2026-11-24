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

package com.davils.konnect.ipc.core.framing

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcWriteResult

/**
 * A [connection] that sends and receives whole frames of [framing].
 *
 * It shares the connection beneath: closing one closes the other. It is safe to use from several coroutines as the
 * connection is.
 *
 * @param F the type of a frame.
 * @property connection the connection beneath.
 * @property framing how bytes are split into frames.
 * @since 1.0.0
 */
public class IpcFramedConnection<F> internal constructor(
    public val connection: IpcConnection,
    public val framing: IpcFraming<F>,
) {
    /**
     * The process on the other side.
     *
     * @since 1.0.0
     */
    public val peer: IpcPeer get() = connection.peer

    /**
     * Whether the connection beneath is still open.
     *
     * @since 1.0.0
     */
    public val isOpen: Boolean get() = connection.isOpen

    /**
     * Receives the next frame.
     *
     * @return the frame, or why there is none.
     * @since 1.0.0
     */
    public suspend fun receive(): IpcFrameResult<F> = framing.receive(connection)

    /**
     * Sends [frame].
     *
     * @param frame the frame to send.
     * @return what writing the frame produced.
     * @throws IllegalArgumentException if the frame exceeds the limit of the framing; nothing is sent.
     * @since 1.0.0
     */
    public suspend fun send(frame: F): IpcWriteResult {
        val bytes = framing.encode(frame)
        return connection.write(bytes)
    }

    /**
     * Closes the connection beneath.
     *
     * @since 1.0.0
     */
    public fun close() {
        connection.close()
    }

    /**
     * Shows the connection and the framing.
     *
     * @return the framed connection as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcFramedConnection($connection, $framing)"
}
