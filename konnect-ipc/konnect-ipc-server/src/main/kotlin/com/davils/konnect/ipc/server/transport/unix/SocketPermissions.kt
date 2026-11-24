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

package com.davils.konnect.ipc.server.transport.unix

import com.davils.konnect.ipc.core.access.IpcAccess
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

internal object SocketPermissions {
    val staging: Set<PosixFilePermission> = PosixFilePermissions.fromString("rwx------")

    private val sharedDirectory: Set<PosixFilePermission> = PosixFilePermissions.fromString("rwx--x--x")

    private val privateSocket: Set<PosixFilePermission> = PosixFilePermissions.fromString("rw-------")

    private val sharedSocket: Set<PosixFilePermission> = PosixFilePermissions.fromString("rw-rw-rw-")

    fun directory(access: IpcAccess): Set<PosixFilePermission> {
        if (access.isCurrentUserOnly) return staging
        return sharedDirectory
    }

    fun socket(access: IpcAccess): Set<PosixFilePermission> {
        if (access.isCurrentUserOnly) return privateSocket
        return sharedSocket
    }
}
