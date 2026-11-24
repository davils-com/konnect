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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

private const val SOCKET_SUFFIX: String = ".sock"

/**
 * A file system in memory with Unix sockets that fail where they were told to.
 *
 * It is safe to use from several threads.
 *
 * @param existing the paths that exist from the start.
 * @param owner the owner of every path; [FakeIpcEnvironment.USER] by default.
 * @param isWritable whether every path is writable.
 * @param directoryFailure what creating a directory throws, or `null` for nothing.
 * @param bindFailure what binding throws, or `null` for nothing.
 * @param restrictFailure what restricting a socket, a path ending in `.sock`, throws, or `null` for nothing.
 * @param moveFailure what moving a path throws, or `null` for nothing.
 * @param connecting creates the socket a connect returns.
 * @property server the server socket every bind returns.
 * @since 1.0.0
 */
@DavilsInternal
public class FakeUnixSocketApi(
    existing: Set<Path> = emptySet(),
    private val owner: String = FakeIpcEnvironment.USER,
    private val isWritable: Boolean = true,
    private val directoryFailure: IOException? = null,
    private val bindFailure: IOException? = null,
    private val restrictFailure: IOException? = null,
    private val moveFailure: IOException? = null,
    private val connecting: (Path) -> UnixSocket = { FakeUnixSocket() },
    public val server: FakeUnixServerSocket = FakeUnixServerSocket(),
) : UnixSocketApi {
    private val paths: MutableSet<Path> = ConcurrentHashMap.newKeySet()

    private val restricted: MutableMap<Path, Set<PosixFilePermission>> = ConcurrentHashMap()

    private val deletedPaths: MutableList<Path> = CopyOnWriteArrayList()

    private val movedPaths: MutableList<Pair<Path, Path>> = CopyOnWriteArrayList()

    init {
        paths.addAll(existing)
    }

    /**
     * The permissions each path was restricted to.
     *
     * @since 1.0.0
     */
    public val permissions: Map<Path, Set<PosixFilePermission>> get() = restricted.immutableMap()

    /**
     * The paths that were deleted, in order.
     *
     * @since 1.0.0
     */
    public val deleted: List<Path> get() = deletedPaths.immutableList()

    /**
     * The moves that succeeded, in order, each as its source and its target.
     *
     * @since 1.0.0
     */
    public val moved: List<Pair<Path, Path>> get() = movedPaths.immutableList()

    override fun exists(path: Path): Boolean = path in paths

    override fun isWritable(path: Path): Boolean = isWritable

    override fun createDirectories(path: Path) {
        if (directoryFailure != null) throw directoryFailure
        paths.add(path)
    }

    override fun owner(path: Path): String = owner

    override fun restrict(path: Path, permissions: Set<PosixFilePermission>) {
        val name = path.fileName.toString()
        val isSocket = name.endsWith(SOCKET_SUFFIX)
        if (restrictFailure != null && isSocket) throw restrictFailure
        restricted[path] = permissions
    }

    override fun delete(path: Path) {
        deletedPaths.add(path)
        paths.remove(path)
    }

    override fun move(source: Path, target: Path) {
        if (moveFailure != null) throw moveFailure
        paths.remove(source)
        paths.add(target)
        movedPaths.add(source to target)
    }

    override fun bind(path: Path): UnixServerSocket {
        if (bindFailure != null) throw bindFailure
        paths.add(path)
        return server
    }

    override fun connect(path: Path): UnixSocket = connecting(path)
}
