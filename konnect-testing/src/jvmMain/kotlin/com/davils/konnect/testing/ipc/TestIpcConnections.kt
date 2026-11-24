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

import com.davils.konnect.ipc.core.connection.IpcConnection

private const val HASH_FACTOR: Int = 31

/**
 * The two ends of a test connection: what the [server] and what the [client] holds.
 *
 * @property server the end of the server, whose peer is the client.
 * @property client the end of the client, whose peer is the server.
 * @since 1.0.0
 */
public class TestIpcConnections(public val server: IpcConnection, public val client: IpcConnection) {
    /**
     * Tells whether [other] holds the same two connections.
     *
     * @param other the value to compare with.
     * @return `true` if [other] holds the same connections.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is TestIpcConnections && server == other.server && client == other.client

    /**
     * Computes a hash code from the two connections.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * server.hashCode() + client.hashCode()

    /**
     * Shows the two connections.
     *
     * @return the connections as text.
     * @since 1.0.0
     */
    override fun toString(): String = "TestIpcConnections(server=$server, client=$client)"
}
