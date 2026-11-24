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

package com.davils.konnect.ipc.core.connection

import kotlinx.io.bytestring.ByteString

/**
 * What reading from an [IpcConnection] produced.
 *
 * @since 1.0.0
 */
public sealed interface IpcReadResult {
    /**
     * The bytes the other side sent.
     *
     * @property bytes the bytes that were read; never more than were asked for.
     * @since 1.0.0
     */
    public class Received(public val bytes: ByteString) : IpcReadResult {
        /**
         * Tells whether [other] received the same bytes.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Received && bytes == other.bytes

        /**
         * Computes a hash code from the bytes.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = bytes.hashCode()

        /**
         * Shows how many bytes were received, never the bytes themselves, which came from the other side.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Received(size=${bytes.size})"
    }

    /**
     * The other side has closed its output or the connection is closed; no more bytes will arrive.
     *
     * @since 1.0.0
     */
    public data object Closed : IpcReadResult

    /**
     * The operating system failed the read; the connection is closed.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcReadResult {
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
