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

package com.davils.konnect.ipc.server

import com.davils.konfetti.core.Event
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress

private const val HASH_FACTOR: Int = 31

/**
 * What a server did, as it publishes it on [IpcServerTopics.EVENTS].
 *
 * Events are immutable. They carry the address of the server and, where a peer is concerned, the peer as the
 * operating system reported it; neither holds text a peer could choose.
 *
 * @since 1.0.0
 */
public sealed interface IpcServerEvent : Event {
    /**
     * The address of the server.
     *
     * @since 1.0.0
     */
    public val address: IpcAddress

    /**
     * The server listens on its address.
     *
     * @property address the address of the server.
     * @since 1.0.0
     */
    public class Bound(override val address: IpcAddress) : IpcServerEvent {
        /**
         * Tells whether [other] is the same event.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal event.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Bound && address == other.address

        /**
         * Computes a hash code from the address.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = address.hashCode()

        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Bound($address)"
    }

    /**
     * The server accepted a peer that may connect.
     *
     * @property address the address of the server.
     * @property peer the process on the other side.
     * @since 1.0.0
     */
    public class Accepted(override val address: IpcAddress, public val peer: IpcPeer) : IpcServerEvent {
        /**
         * Tells whether [other] is the same event.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal event.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Accepted && address == other.address && peer == other.peer

        /**
         * Computes a hash code from the address and the peer.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * address.hashCode() + peer.hashCode()

        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Accepted($address, $peer)"
    }

    /**
     * The server refused a peer whose user may not connect or could not be read, and closed its connection.
     *
     * @property address the address of the server.
     * @property peer the process that was refused.
     * @since 1.0.0
     */
    public class PeerRejected(override val address: IpcAddress, public val peer: IpcPeer) : IpcServerEvent {
        /**
         * Tells whether [other] is the same event.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal event.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is PeerRejected && address == other.address && peer == other.peer

        /**
         * Computes a hash code from the address and the peer.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * address.hashCode() + peer.hashCode()

        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "PeerRejected($address, $peer)"
    }

    /**
     * The server stopped listening for good.
     *
     * @property address the address of the server.
     * @since 1.0.0
     */
    public class Closed(override val address: IpcAddress) : IpcServerEvent {
        /**
         * Tells whether [other] is the same event.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal event.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Closed && address == other.address

        /**
         * Computes a hash code from the address.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = address.hashCode()

        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Closed($address)"
    }
}
