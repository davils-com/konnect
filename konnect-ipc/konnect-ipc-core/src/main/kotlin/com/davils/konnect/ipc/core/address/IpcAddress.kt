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

package com.davils.konnect.ipc.core.address

import com.davils.konnect.ipc.core.Limits
import java.nio.file.Path

private const val HASH_FACTOR: Int = 31
private const val PIPE_SEPARATOR: Char = '\\'

/**
 * Where a local server listens and a local client connects to.
 *
 * A [Named] address is portable: it becomes a Unix domain socket in a directory Konnect manages on Unix and a named
 * pipe in the namespace of Konnect on Windows. A [WindowsPipe] or a [UnixSocket] reaches the pipe or the socket of
 * another program as it is. Every address is immutable and checked when it is created.
 *
 * @since 1.0.0
 */
public sealed interface IpcAddress {
    /**
     * A portable address, made of a [name] and the [scope] it belongs to.
     *
     * @property name the name of the address.
     * @property scope who the address belongs to; [IpcScope.User] unless it is given.
     * @since 1.0.0
     */
    public class Named(public val name: IpcName, public val scope: IpcScope = IpcScope.User) : IpcAddress {
        /**
         * Tells whether [other] is a named address with the same name and scope.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal address.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Named && name == other.name && scope == other.scope

        /**
         * Computes a hash code from the name and the scope.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * name.hashCode() + scope.hashCode()

        /**
         * Shows the address as its scope and its name, such as `user:helper`.
         *
         * @return the address as text.
         * @since 1.0.0
         */
        override fun toString(): String {
            val label = scope.name.lowercase()
            return "$label:$name"
        }
    }

    /**
     * The named pipe of another program, such as `discord-ipc-0`, which lives at `\\.\pipe\<name>`.
     *
     * The name holds from 1 to [Limits.MAX_PIPE_NAME_LENGTH] characters, none of them a control character or a
     * backslash, which Windows reserves as the separator of pipe names.
     *
     * @property name the name of the pipe, without the `\\.\pipe\` prefix.
     * @throws IllegalArgumentException if [name] is not a pipe name; the message never quotes it.
     * @since 1.0.0
     */
    public class WindowsPipe(public val name: String) : IpcAddress {
        init {
            require(isValid(name)) { "Not a pipe name: it must hold printable characters without a backslash" }
        }

        /**
         * Tells whether [other] is a pipe address with the same name.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal address.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is WindowsPipe && name == other.name

        /**
         * Computes a hash code from the name.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = name.hashCode()

        /**
         * Shows the address as `pipe:<name>`.
         *
         * @return the address as text.
         * @since 1.0.0
         */
        override fun toString(): String = "pipe:$name"

        /**
         * Checks pipe names.
         *
         * @since 1.0.0
         */
        public companion object {
            /**
             * Tells whether [name] is the name of a pipe.
             *
             * A name longer than [Limits.MAX_PIPE_NAME_LENGTH] is refused before its characters are looked at.
             *
             * @param name the name to check; it may come from an attacker.
             * @return `true` if [name] is a pipe name.
             * @since 1.0.0
             */
            public fun isValid(name: String): Boolean {
                if (name.length !in 1..Limits.MAX_PIPE_NAME_LENGTH) return false
                return name.none { character -> character.isISOControl() || character == PIPE_SEPARATOR }
            }
        }
    }

    /**
     * The Unix domain socket of another program, at an absolute [path].
     *
     * The path is absolute, holds no control character and fits [Limits.MAX_SOCKET_PATH_BYTES] bytes in UTF-8.
     *
     * @property path the path of the socket.
     * @throws IllegalArgumentException if [path] is not a socket path; the message never quotes it.
     * @since 1.0.0
     */
    public class UnixSocket(public val path: Path) : IpcAddress {
        init {
            require(isValid(path)) {
                "Not a socket path: it must be absolute and fit ${Limits.MAX_SOCKET_PATH_BYTES} bytes"
            }
        }

        /**
         * Tells whether [other] is a socket address with the same path.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal address.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is UnixSocket && path == other.path

        /**
         * Computes a hash code from the path.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = path.hashCode()

        /**
         * Shows the address as `unix:<path>`.
         *
         * @return the address as text.
         * @since 1.0.0
         */
        override fun toString(): String = "unix:$path"

        /**
         * Checks socket paths.
         *
         * @since 1.0.0
         */
        public companion object {
            /**
             * Tells whether [path] can be the path of a socket.
             *
             * A path longer than [Limits.MAX_SOCKET_PATH_BYTES] characters is refused before it is encoded.
             *
             * @param path the path to check; it may come from an attacker.
             * @return `true` if [path] is absolute, holds no control character and fits the limit.
             * @since 1.0.0
             */
            public fun isValid(path: Path): Boolean {
                val text = path.toString()
                val isShortAbsolute = path.isAbsolute && text.length <= Limits.MAX_SOCKET_PATH_BYTES
                if (!isShortAbsolute || text.any(Char::isISOControl)) return false
                return text.encodeToByteArray().size <= Limits.MAX_SOCKET_PATH_BYTES
            }
        }
    }
}
