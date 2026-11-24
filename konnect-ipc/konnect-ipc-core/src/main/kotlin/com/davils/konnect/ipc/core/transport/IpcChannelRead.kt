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

package com.davils.konnect.ipc.core.transport

import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.kore.core.annotation.DavilsInternal

/**
 * What one read of an [IpcChannel] produced.
 *
 * @since 1.0.0
 */
@DavilsInternal
public sealed interface IpcChannelRead {
    /**
     * Bytes arrived.
     *
     * @property count how many bytes were read.
     * @since 1.0.0
     */
    public class Read(public val count: Int) : IpcChannelRead {
        /**
         * Tells whether [other] read as many bytes.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal read.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Read && count == other.count

        /**
         * Computes a hash code from the count.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = count

        /**
         * Shows the count.
         *
         * @return the read as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Read(count=$count)"
    }

    /**
     * The stream ended; no more bytes will arrive.
     *
     * @since 1.0.0
     */
    public data object Ended : IpcChannelRead

    /**
     * The operating system failed the read.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcChannelRead {
        /**
         * Tells whether [other] failed with the same error.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal read.
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
         * @return the read as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($error)"
    }
}
