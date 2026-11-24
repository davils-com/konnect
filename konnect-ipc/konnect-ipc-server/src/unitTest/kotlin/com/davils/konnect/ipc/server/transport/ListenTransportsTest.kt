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

package com.davils.konnect.ipc.server.transport

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.server.fixture.HELPER
import com.davils.konnect.ipc.server.fixture.refusal
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.unix.UnixListenTransport
import com.davils.konnect.ipc.server.transport.windows.NamedPipeListenTransport
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.HostPlatform
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.StandardTestDispatcher

private const val BUFFER: Int = 1024

class ListenTransportsTest : FunSpec({
    context("the transport of a platform") {
        test("is a named pipe on windows") {
            val windows = HostPlatform.of(osName = "Windows 11", osArch = "amd64")

            val transport = ListenTransports.forPlatform(windows, StandardTestDispatcher())

            transport.shouldBeInstanceOf<NamedPipeListenTransport>()
        }

        test("is a unix domain socket on linux, macos and freebsd") {
            listOf("Linux", "Mac OS X", "FreeBSD").forEach { system ->
                val platform = HostPlatform.of(osName = system, osArch = "aarch64")

                val transport = ListenTransports.forPlatform(platform, StandardTestDispatcher())

                transport.shouldBeInstanceOf<UnixListenTransport>()
            }
        }

        test("of the running platform is the one of its operating system") {
            val current = ListenTransports.forCurrentPlatform(StandardTestDispatcher())
            val expected = ListenTransports.forPlatform(HostPlatform.current(), StandardTestDispatcher())

            current::class shouldBe expected::class
        }
    }

    context("an unknown platform") {
        test("offers no local connections") {
            val transport = ListenTransports.forPlatform(platform = null, dispatcher = StandardTestDispatcher())

            val listened = transport.listen(HELPER, IpcAccess.CURRENT_USER, BUFFER)

            transport.currentUser.shouldBeNull()
            listened.refusal().shouldBeInstanceOf<IpcBindResult.Unavailable>()
        }
    }
})
