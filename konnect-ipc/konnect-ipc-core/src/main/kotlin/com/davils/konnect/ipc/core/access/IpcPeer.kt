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

package com.davils.konnect.ipc.core.access

private const val HASH_FACTOR: Int = 31
private const val UNREAD: String = "unknown"

/**
 * The process on the other side of a connection, as far as the operating system could tell.
 *
 * Instances are immutable.
 *
 * @property user the user the process runs as, or `null` if it could not be read.
 * @property processId the identifier of the process, or `null` if it could not be read; Unix domain sockets of the
 *   JDK report none.
 * @since 1.0.0
 */
public class IpcPeer(public val user: IpcUser?, public val processId: Long?) {
    /**
     * Tells whether [other] is a peer with the same user and process.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal peer.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is IpcPeer && user == other.user && processId == other.processId

    /**
     * Computes a hash code from the user and the process.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * user.hashCode() + processId.hashCode()

    /**
     * Shows the user and the process of the peer.
     *
     * @return the peer as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcPeer(user=${user ?: UNREAD}, processId=${processId ?: UNREAD})"

    /**
     * The peer of unknown identity.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The peer whose user and process could not be read.
         *
         * @since 1.0.0
         */
        public val UNKNOWN: IpcPeer = IpcPeer(user = null, processId = null)
    }
}
