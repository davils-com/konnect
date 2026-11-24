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

package com.davils.konnect.ipc.client

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcSystemError

/**
 * How an attempt to connect to a local server ended.
 *
 * Every result but [Connected] means that no connection is open. The results are immutable and say what went wrong in
 * constant text or in canonical values, never in text the server or the operating system chose.
 *
 * @since 1.0.0
 */
public sealed interface IpcConnectResult {
    /**
     * The server was reached and is trusted; the caller owns [connection] and closes it.
     *
     * @property connection the open connection.
     * @since 1.0.0
     */
    public class Connected(public val connection: IpcConnection) : IpcConnectResult {
        /**
         * Tells whether [other] holds the same connection.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is a result with the same connection.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Connected && connection == other.connection

        /**
         * Computes a hash code from the connection.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = connection.hashCode()

        /**
         * Shows the connection.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Connected($connection)"
    }

    /**
     * No server listens at the address.
     *
     * @since 1.0.0
     */
    public data object NotFound : IpcConnectResult

    /**
     * The operating system refused to let the process reach the server.
     *
     * @since 1.0.0
     */
    public data object AccessDenied : IpcConnectResult

    /**
     * The server did not accept the connection within the connect timeout.
     *
     * @since 1.0.0
     */
    public data object TimedOut : IpcConnectResult

    /**
     * The server was reached, but its user is not trusted, or could not be read; the connection was closed before a
     * byte was sent.
     *
     * @property peer the process of the server, as far as the operating system could tell.
     * @since 1.0.0
     */
    public class ServerRejected(public val peer: IpcPeer) : IpcConnectResult {
        /**
         * Tells whether [other] rejected the same peer.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is a rejection of an equal peer.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is ServerRejected && peer == other.peer

        /**
         * Computes a hash code from the peer.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = peer.hashCode()

        /**
         * Shows the peer.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "ServerRejected($peer)"
    }

    /**
     * Local connections of this kind cannot be made here, such as a Windows pipe on Linux or a named pipe without
     * its native library.
     *
     * @property reason why, in constant text and canonical addresses.
     * @since 1.0.0
     */
    public class Unavailable(public val reason: String) : IpcConnectResult {
        /**
         * Tells whether [other] gives the same reason.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Unavailable && reason == other.reason

        /**
         * Computes a hash code from the reason.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = reason.hashCode()

        /**
         * Shows the reason.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Unavailable($reason)"
    }

    /**
     * The operating system reported a failure.
     *
     * @property error the failure, in constant text.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcConnectResult {
        /**
         * Tells whether [other] reports the same failure.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Failed && error == other.error

        /**
         * Computes a hash code from the failure.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = error.hashCode()

        /**
         * Shows the failure.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($error)"
    }
}
