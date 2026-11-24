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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

@DavilsPlatformBinding
internal object JdkUnixSocketApi : UnixSocketApi {
    override fun exists(path: Path): Boolean = Files.exists(path)

    override fun isWritable(path: Path): Boolean = Files.isWritable(path)

    override fun createDirectories(path: Path) {
        Files.createDirectories(path)
    }

    override fun owner(path: Path): String {
        val owner = Files.getOwner(path)
        return owner.name
    }

    override fun restrict(path: Path, permissions: Set<PosixFilePermission>) {
        Files.setPosixFilePermissions(path, permissions)
    }

    override fun delete(path: Path) {
        Files.deleteIfExists(path)
    }

    override fun move(source: Path, target: Path) {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
    }

    override fun bind(path: Path): UnixServerSocket {
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        channel.bind(UnixDomainSocketAddress.of(path))
        return JdkUnixServerSocket(channel)
    }

    override fun connect(path: Path): UnixSocket {
        val channel = SocketChannel.open(UnixDomainSocketAddress.of(path))
        return JdkUnixSocket(channel)
    }
}
