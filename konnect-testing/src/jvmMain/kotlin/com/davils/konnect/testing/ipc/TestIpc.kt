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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.transport.memoryChannels
import com.davils.kore.core.annotation.DavilsInternal

/**
 * Connections between two processes that live in memory, for tests of code that reads and writes an
 * [IpcConnection].
 *
 * @since 1.0.0
 */
public object TestIpc {
    /**
     * The peer the client sees on the other side unless another is given: the Unix user `konnect-server`, process 1.
     *
     * @since 1.0.0
     */
    public val SERVER: IpcPeer = IpcPeer(user = IpcUser.UnixUser("konnect-server"), processId = 1L)

    /**
     * The peer the server sees on the other side unless another is given: the Unix user `konnect-client`, process 2.
     *
     * @since 1.0.0
     */
    public val CLIENT: IpcPeer = IpcPeer(user = IpcUser.UnixUser("konnect-client"), processId = 2L)

    /**
     * Creates a connected pair of connections in memory.
     *
     * What one end writes the other reads; closing the output of one ends what the other reads, and closing one
     * ends both. Writes never wait, so the connections buffer without a bound.
     *
     * @param server the peer the client sees.
     * @param client the peer the server sees.
     * @param bufferSize the buffer size of both connections, from [Limits.MIN_BUFFER_SIZE] to
     *   [Limits.MAX_BUFFER_SIZE]; [Limits.DEFAULT_BUFFER_SIZE] by default.
     * @return the two ends.
     * @throws IllegalArgumentException if [bufferSize] is outside of its bounds.
     * @since 1.0.0
     */
    public fun connections(
        server: IpcPeer = SERVER,
        client: IpcPeer = CLIENT,
        bufferSize: Int = Limits.DEFAULT_BUFFER_SIZE,
    ): TestIpcConnections {
        val (serverSide, clientSide) = memoryChannels(first = server, second = client)
        return TestIpcConnections(
            server = IpcConnection(serverSide, bufferSize),
            client = IpcConnection(clientSide, bufferSize),
        )
    }
}
