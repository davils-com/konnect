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

private const val HASH_FACTOR: Int = 31
private const val UNMANAGED: String = "none"

/**
 * Where the socket of an address lives.
 *
 * @property path the path of the socket.
 * @property managedDirectory the directory Konnect creates, restricts and checks for the socket, or `null` for the
 *   explicit path of another program, whose directory Konnect leaves alone.
 * @since 1.0.0
 */
@DavilsInternal
public class SocketLocation(public val path: Path, public val managedDirectory: Path?) {
    /**
     * Tells whether [other] is the same location.
     *
     * @param other the value to compare with.
     * @return `true` if [other] has the same path and directory.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is SocketLocation && path == other.path && managedDirectory == other.managedDirectory

    /**
     * Computes a hash code from the path and the directory.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * path.hashCode() + managedDirectory.hashCode()

    /**
     * Shows the path and the directory.
     *
     * @return the location as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SocketLocation(path=$path, managedDirectory=${managedDirectory ?: UNMANAGED})"
}
