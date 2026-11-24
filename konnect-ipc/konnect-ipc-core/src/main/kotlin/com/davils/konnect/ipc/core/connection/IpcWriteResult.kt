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

/**
 * What writing to an [IpcConnection], or closing its output, produced.
 *
 * @since 1.0.0
 */
public sealed interface IpcWriteResult {
    /**
     * Every byte was handed to the operating system, or the output was closed.
     *
     * @since 1.0.0
     */
    public data object Written : IpcWriteResult

    /**
     * The connection or its output is closed, or the other side has gone; nothing was written.
     *
     * @since 1.0.0
     */
    public data object Closed : IpcWriteResult

    /**
     * The operating system failed the write; the connection is closed.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcWriteResult {
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
