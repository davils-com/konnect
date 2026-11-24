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

package com.davils.konnect.ipc.client.transport

import com.davils.konnect.ipc.client.transport.unix.UnixConnectTransport
import com.davils.konnect.ipc.client.transport.windows.NamedPipeConnectTransport
import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.HostPlatform
import com.davils.kore.core.platform.OperatingSystem
import kotlinx.coroutines.CoroutineDispatcher

internal object ConnectTransports {
    fun forCurrentPlatform(dispatcher: CoroutineDispatcher): ConnectTransport =
        forPlatform(HostPlatform.current(), dispatcher)

    fun forPlatform(platform: HostPlatform?, dispatcher: CoroutineDispatcher): ConnectTransport {
        if (platform == null) return UnsupportedConnectTransport
        return when (platform.operatingSystem) {
            OperatingSystem.Windows -> NamedPipeConnectTransport(
                library = IpcPlatform.namedPipes,
                dispatcher = dispatcher,
                currentProcessId = IpcPlatform.currentProcessId,
            )

            OperatingSystem.Linux,
            OperatingSystem.MacOs,
            OperatingSystem.FreeBsd,
            -> UnixConnectTransport(
                api = IpcPlatform.unixSockets,
                environment = IpcPlatform.environment,
                isMacOs = platform.operatingSystem == OperatingSystem.MacOs,
                dispatcher = dispatcher,
            )
        }
    }
}
