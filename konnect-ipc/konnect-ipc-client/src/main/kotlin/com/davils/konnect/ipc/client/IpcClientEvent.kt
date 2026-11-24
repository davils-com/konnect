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

import com.davils.konfetti.core.Event
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress

private const val HASH_FACTOR: Int = 31

/**
 * What a client publishes on the topic [IpcClientTopics.EVENTS] of the event bus of its telemetry.
 *
 * A client publishes only when its telemetry names an event bus, declares the topic on the bus if it is missing and
 * never waits for the bus: an event the bus cannot take at once is dropped. The events are immutable.
 *
 * @since 1.0.0
 */
public sealed interface IpcClientEvent : Event {
    /**
     * A client left a server whose user it does not trust, or whose user could not be read, before it sent a byte.
     *
     * @property address the address the client connected to.
     * @property peer the process of the server, as far as the operating system could tell.
     * @since 1.0.0
     */
    public class ServerRejected(public val address: IpcAddress, public val peer: IpcPeer) : IpcClientEvent {
        /**
         * Tells whether [other] is a rejection of the same peer at the same address.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal event.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is ServerRejected && address == other.address && peer == other.peer

        /**
         * Computes a hash code from the address and the peer.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * address.hashCode() + peer.hashCode()

        /**
         * Shows the address and the peer.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "ServerRejected(address=$address, peer=$peer)"
    }
}
