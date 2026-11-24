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

package com.davils.konnect.ipc.server.transport.unix

import com.davils.konnect.ipc.core.transport.unix.UnixServerSocket
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.testing.ipc.FakeUnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

internal class RecordingUnixSocketApi(
    val fake: FakeUnixSocketApi = FakeUnixSocketApi(),
    private val owners: Map<Path, String> = emptyMap(),
    private val restrictFailures: Set<Path> = emptySet(),
    private val onBind: (FakeUnixSocketApi) -> Unit = {},
) : UnixSocketApi by fake {
    val operations: MutableList<String> = mutableListOf()

    override fun owner(path: Path): String = owners[path] ?: fake.owner(path)

    override fun restrict(path: Path, permissions: Set<PosixFilePermission>) {
        operations += "restrict $path"
        if (path in restrictFailures) throw IOException("denied")
        fake.restrict(path, permissions)
    }

    override fun bind(path: Path): UnixServerSocket {
        operations += "bind $path"
        val server = fake.bind(path)
        onBind(fake)
        return server
    }

    override fun move(source: Path, target: Path) {
        operations += "move $source $target"
        fake.move(source, target)
    }
}
