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

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.fixture.HELPER
import com.davils.konnect.ipc.client.fixture.refusal
import com.davils.konnect.ipc.client.transport.unix.UnixConnectTransport
import com.davils.konnect.ipc.client.transport.windows.NamedPipeConnectTransport
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.HostPlatform
import com.davils.kore.core.platform.OperatingSystem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.time.Duration.Companion.seconds

private const val BUFFER: Int = 1024
private const val X64: String = "amd64"

class ConnectTransportsTest : FunSpec({
    context("the transport of a platform") {
        test("is a named pipe on windows") {
            val windows = HostPlatform.of(osName = "Windows 11", osArch = X64)

            val transport = ConnectTransports.forPlatform(windows, StandardTestDispatcher())

            transport.shouldBeInstanceOf<NamedPipeConnectTransport>()
        }

        test("is a unix domain socket on linux, macos and freebsd") {
            listOf("Linux" to X64, "Mac OS X" to "aarch64", "FreeBSD" to X64).forEach { (system, arch) ->
                val platform = HostPlatform.of(osName = system, osArch = arch)

                val transport = ConnectTransports.forPlatform(platform, StandardTestDispatcher())

                transport.shouldBeInstanceOf<UnixConnectTransport>()
            }
        }

        test("of the running platform is known") {
            val transport = ConnectTransports.forCurrentPlatform(StandardTestDispatcher())
            val isWindows = HostPlatform.current()?.operatingSystem == OperatingSystem.Windows

            if (isWindows) transport.shouldBeInstanceOf<NamedPipeConnectTransport>()
            if (!isWindows) transport.shouldBeInstanceOf<UnixConnectTransport>()
        }
    }

    context("an unknown platform") {
        test("offers no local connections") {
            val transport = ConnectTransports.forPlatform(platform = null, dispatcher = StandardTestDispatcher())

            val connected = transport.connect(HELPER, 1.seconds, BUFFER)

            transport.currentUser.shouldBeNull()
            connected.refusal().shouldBeInstanceOf<IpcConnectResult.Unavailable>()
        }
    }
})
