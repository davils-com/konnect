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

package com.davils.konnect.ipc.core.framing

import com.davils.konnect.ipc.core.connection.IpcSystemError

private const val HASH_FACTOR: Int = 31

/**
 * What receiving a frame produced.
 *
 * @param F the type of a frame.
 * @since 1.0.0
 */
public sealed interface IpcFrameResult<out F> {
    /**
     * A whole frame.
     *
     * @param F the type of the frame.
     * @property frame the frame.
     * @since 1.0.0
     */
    public class Received<out F>(public val frame: F) : IpcFrameResult<F> {
        /**
         * Tells whether [other] received an equal frame.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Received<*> && frame == other.frame

        /**
         * Computes a hash code from the frame.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = frame.hashCode()

        /**
         * Shows that a frame was received, never the frame itself, which came from the other side.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Received"
    }

    /**
     * The connection closed, before the frame or in its middle; a partial frame is never returned.
     *
     * @since 1.0.0
     */
    public data object Closed : IpcFrameResult<Nothing>

    /**
     * The other side announced a frame larger than the framing accepts; the connection is closed.
     *
     * @property size the size the other side announced, in bytes.
     * @property maxFrameSize the largest frame of the framing, in bytes.
     * @since 1.0.0
     */
    public class TooLarge(public val size: Long, public val maxFrameSize: Int) : IpcFrameResult<Nothing> {
        /**
         * Tells whether [other] refused the same size for the same limit.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal result.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is TooLarge && size == other.size && maxFrameSize == other.maxFrameSize

        /**
         * Computes a hash code from the size and the limit.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * size.hashCode() + maxFrameSize

        /**
         * Shows the size and the limit.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "TooLarge(size=$size, maxFrameSize=$maxFrameSize)"
    }

    /**
     * The operating system failed the read; the connection is closed.
     *
     * @property error what failed.
     * @since 1.0.0
     */
    public class Failed(public val error: IpcSystemError) : IpcFrameResult<Nothing> {
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
