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

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcScope
import com.davils.konnect.ipc.core.transport.environment.IpcEnvironment
import com.davils.kore.core.annotation.DavilsInternal
import java.nio.file.Path

private const val RUNTIME_DIRECTORY: String = "XDG_RUNTIME_DIR"
private const val DIRECTORY: String = "konnect"
private const val SUFFIX: String = ".sock"
private const val PATH_SEPARATOR: Char = '/'
private val relativeNames: Set<String> = setOf(".", "..")

/**
 * Finds where the socket of an address lives.
 *
 * A named address of the user lives in `$XDG_RUNTIME_DIR/konnect` on Linux, or in `<tmpdir>/konnect-<user>` when
 * that variable is not set, and in `<tmpdir>/konnect` on macOS, whose temporary directory belongs to the user. A
 * named address of the system lives in `/run/konnect` on Linux and `/var/run/konnect` on macOS. The socket is called
 * `<name>.sock`.
 *
 * @param environment the environment of the process.
 * @param isMacOs whether the process runs on macOS.
 * @since 1.0.0
 */
@DavilsInternal
public class SocketLocations(private val environment: IpcEnvironment, private val isMacOs: Boolean) {
    /**
     * Finds where the socket of [address] lives.
     *
     * @param address the address.
     * @return the location, or `null` for a Windows pipe, for a path that would not fit a socket address and for a
     *   user name that cannot be part of a directory name.
     * @since 1.0.0
     */
    public fun of(address: IpcAddress): SocketLocation? = when (address) {
        is IpcAddress.Named -> named(address)
        is IpcAddress.UnixSocket -> SocketLocation(address.path, managedDirectory = null)
        is IpcAddress.WindowsPipe -> null
    }

    private fun named(address: IpcAddress.Named): SocketLocation? {
        val directory = directoryOf(address.scope)
        val path = directory?.resolve("${address.name}$SUFFIX")
        if (path == null || !IpcAddress.UnixSocket.isValid(path)) return null
        return SocketLocation(path, managedDirectory = directory)
    }

    private fun directoryOf(scope: IpcScope): Path? = when (scope) {
        IpcScope.User -> userDirectory()
        IpcScope.System -> systemDirectory()
    }

    private fun systemDirectory(): Path {
        if (isMacOs) return Path.of("/var/run", DIRECTORY)
        return Path.of("/run", DIRECTORY)
    }

    private fun userDirectory(): Path? {
        val runtime = environment.variable(RUNTIME_DIRECTORY)
        if (runtime != null && !isMacOs) return Path.of(runtime, DIRECTORY)
        val name = privateDirectoryName()
        return name?.let { directory -> environment.temporaryDirectory.resolve(directory) }
    }

    private fun privateDirectoryName(): String? {
        if (isMacOs) return DIRECTORY
        val user = environment.userName
        val isSafe = IpcUser.UnixUser.isValid(user) && PATH_SEPARATOR !in user && user !in relativeNames
        return "$DIRECTORY-$user".takeIf { isSafe }
    }
}
