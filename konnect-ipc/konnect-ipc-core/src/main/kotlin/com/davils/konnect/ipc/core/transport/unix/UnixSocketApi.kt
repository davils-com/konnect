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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.kore.core.annotation.DavilsInternal
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * The file system and the Unix domain sockets a Unix transport works with.
 *
 * Every function blocks the calling thread and may throw the [java.io.IOException] of the JDK.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface UnixSocketApi {
    /**
     * Tells whether something exists at [path].
     *
     * @param path the path.
     * @return `true` if [path] exists.
     * @since 1.0.0
     */
    public fun exists(path: Path): Boolean

    /**
     * Tells whether the process may write to [path].
     *
     * @param path the path.
     * @return `true` if [path] is writable.
     * @since 1.0.0
     */
    public fun isWritable(path: Path): Boolean

    /**
     * Creates the directory [path] and every missing parent.
     *
     * @param path the directory.
     * @since 1.0.0
     */
    public fun createDirectories(path: Path)

    /**
     * Reads the name of the user that owns [path].
     *
     * @param path the path.
     * @return the user name of the owner.
     * @since 1.0.0
     */
    public fun owner(path: Path): String

    /**
     * Sets the permissions of [path] to exactly [permissions].
     *
     * @param path the path.
     * @param permissions the permissions.
     * @since 1.0.0
     */
    public fun restrict(path: Path, permissions: Set<PosixFilePermission>)

    /**
     * Deletes [path] if it exists.
     *
     * @param path the path.
     * @since 1.0.0
     */
    public fun delete(path: Path)

    /**
     * Moves [source] to [target] in one atomic step of the file system, so no process ever sees a moment in which
     * [target] exists half-way; both paths lie on the same file system.
     *
     * @param source the path to move, such as a socket bound in a private directory.
     * @param target the path it gets; whether an existing [target] is replaced depends on the operating system, so a
     *   caller checks first that nothing is there.
     * @throws java.nio.file.AtomicMoveNotSupportedException if the file system cannot move atomically.
     * @since 1.0.0
     */
    public fun move(source: Path, target: Path)

    /**
     * Binds a server socket to [path].
     *
     * @param path the path of the socket; nothing may exist there yet.
     * @return the bound socket.
     * @since 1.0.0
     */
    public fun bind(path: Path): UnixServerSocket

    /**
     * Connects to the server socket at [path].
     *
     * @param path the path of the socket.
     * @return the connected socket.
     * @since 1.0.0
     */
    public fun connect(path: Path): UnixSocket
}
