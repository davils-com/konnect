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

package com.davils.konnect.ipc.core

import com.davils.konnect.ipc.core.transport.environment.IpcEnvironment
import com.davils.konnect.ipc.core.transport.environment.SystemIpcEnvironment
import com.davils.konnect.ipc.core.transport.unix.JdkUnixSocketApi
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.core.transport.windows.NamedPipeApi
import com.davils.konnect.ipc.core.transport.windows.NativeNamedPipeLoader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import com.davils.kore.core.platform.NativeLibrary

/**
 * The primitives of the operating system that the clients and servers of Konnect are built on.
 *
 * It holds no state of its own except the native library of named pipes, which is loaded once on first use and
 * keeps the outcome of that load.
 *
 * @since 1.0.0
 */
@DavilsInternal
@DavilsPlatformBinding
public object IpcPlatform {
    /**
     * The environment of the running process: its user, its temporary directory and its environment variables.
     *
     * @since 1.0.0
     */
    public val environment: IpcEnvironment get() = SystemIpcEnvironment

    /**
     * The Unix domain sockets of the JDK.
     *
     * @since 1.0.0
     */
    public val unixSockets: UnixSocketApi get() = JdkUnixSocketApi

    /**
     * The named pipes of Windows, loaded from the native library packaged with this module on first use.
     *
     * @since 1.0.0
     */
    public val namedPipes: NativeLibrary<NamedPipeApi> by lazy { NativeLibrary(NativeNamedPipeLoader) }

    /**
     * The identifier of the running process.
     *
     * @since 1.0.0
     */
    public val currentProcessId: Long get() = ProcessHandle.current().pid()
}
