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

package com.davils.konnect.ipc.server.outcome

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.connection.IpcSystemError

/**
 * What accepting the next connection of a server produced.
 *
 * @since 1.0.0
 */
public sealed interface IpcAcceptResult {
    /**
     * A peer that may connect was accepted.
     *
     * @property connection the connection; the caller owns it and closes it, which frees its place among the
     *   connections of the server.
     * @since 1.0.0
     */
    public class Accepted(public val connection: IpcConnection) : IpcAcceptResult {
        /**
         * Tells whether [other] accepted the same connection.
         *
         * @param other the value to compare with.
         * @return `true` if [other] holds the same connection.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Accepted && connection == other.connection

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
        override fun toString(): String = "Accepted($connection)"
    }

    /**
     * A peer connected whose user may not connect or could not be read; its connection was closed.
     *
     * @property peer the process that was refused.
     * @since 1.0.0
     */
    public class PeerRejected(public val peer: IpcPeer) : IpcAcceptResult {
        /**
         * Tells whether [other] refused the same peer.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is PeerRejected && peer == other.peer

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
        override fun toString(): String = "PeerRejected($peer)"
    }

    /**
     * The server does not listen: it was never bound, or it was closed.
     *
     * @since 1.0.0
     */
    public data object NotListening : IpcAcceptResult

    /**
     * The operating system failed the accept; the server keeps listening.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcAcceptResult {
        /**
         * Tells whether [other] failed with the same error.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Failed && error == other.error

        /**
         * Computes a hash code from the error.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = error.hashCode()

        /**
         * Shows the error.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($error)"
    }
}
