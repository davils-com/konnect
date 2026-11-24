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

import com.davils.konnect.ipc.core.connection.IpcSystemError

/**
 * What binding a server to its address produced.
 *
 * @since 1.0.0
 */
public sealed interface IpcBindResult {
    /**
     * The server listens on its address now.
     *
     * @since 1.0.0
     */
    public data object Bound : IpcBindResult

    /**
     * The server listened on its address already; nothing changed.
     *
     * @since 1.0.0
     */
    public data object AlreadyBound : IpcBindResult

    /**
     * The server was closed and never binds again.
     *
     * @since 1.0.0
     */
    public data object Closed : IpcBindResult

    /**
     * Another server listens on the address, or something that cannot be probed is there.
     *
     * @since 1.0.0
     */
    public data object AddressInUse : IpcBindResult

    /**
     * The directory of the address could not be created, or it belongs to another user.
     *
     * @since 1.0.0
     */
    public data object AccessDenied : IpcBindResult

    /**
     * The address cannot be served on this platform, such as a Windows pipe on Linux.
     *
     * @property reason why, in constant text.
     * @since 1.0.0
     */
    public class Unavailable(public val reason: String) : IpcBindResult {
        /**
         * Tells whether [other] is unavailable for the same reason.
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
     * The operating system failed the bind.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcBindResult {
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
