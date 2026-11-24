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

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcName
import com.davils.konnect.ipc.core.address.IpcScope
import com.davils.konnect.ipc.core.fixture.HELPER
import com.davils.konnect.ipc.core.fixture.HELPER_NAME
import com.davils.konnect.ipc.core.fixture.ROOT
import com.davils.konnect.ipc.core.fixture.isUnix
import com.davils.konnect.testing.ipc.FakeIpcEnvironment
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Path

private const val RUNTIME_VARIABLE: String = "XDG_RUNTIME_DIR"
private const val DIRECTORY: String = "konnect"
private const val SOCKET: String = "helper.sock"
private val runtime: Path = ROOT.resolve("run/user/1000")
private val discord: Path = runtime.resolve("discord-ipc-0")
private val macOsTemporary: Path = ROOT.resolve("var/folders/x")
private val linuxWithRuntime = SocketLocations(
    FakeIpcEnvironment(variables = mapOf(RUNTIME_VARIABLE to runtime.toString())),
    isMacOs = false,
)
private val macOs = SocketLocations(
    FakeIpcEnvironment(
        temporaryDirectory = macOsTemporary,
        variables = mapOf(RUNTIME_VARIABLE to runtime.toString()),
    ),
    isMacOs = true,
)
private val system = IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.System)

class SocketLocationsTest : FunSpec({
    context("a named address of the user") {
        test("lives in the runtime directory on linux") {
            linuxWithRuntime.of(HELPER) shouldBe SocketLocation(
                path = runtime.resolve(DIRECTORY).resolve(SOCKET),
                managedDirectory = runtime.resolve(DIRECTORY),
            )
        }

        test("lives in a private temporary directory on linux without a runtime directory") {
            val environment = FakeIpcEnvironment()
            val directory = environment.temporaryDirectory.resolve("konnect-alice")

            SocketLocations(environment, isMacOs = false).of(HELPER)?.path shouldBe directory.resolve(SOCKET)
        }

        test("lives in the temporary directory of the user on macos") {
            macOs.of(HELPER)?.path shouldBe macOsTemporary.resolve(DIRECTORY).resolve(SOCKET)
        }

        test("is unreachable when its path does not fit a socket address") {
            val deep = FakeIpcEnvironment(
                temporaryDirectory = ROOT.resolve("d".repeat(Limits.MAX_SOCKET_PATH_BYTES)),
            )

            val locations = SocketLocations(deep, isMacOs = false)

            locations.of(HELPER).shouldBeNull()
        }

        test("is unreachable when the user name cannot name a directory") {
            listOf("..", ".", "a/b", "", "a\nb").forEach { user ->
                val hostile = SocketLocations(FakeIpcEnvironment(userName = user), isMacOs = false)

                hostile.of(HELPER).shouldBeNull()
            }
        }
    }

    context("a named address of the system") {
        test("lives in the runtime directory of the system").config(enabledIf = { isUnix }) {
            linuxWithRuntime.of(system)?.path shouldBe Path.of("/run/konnect/helper.sock")
            macOs.of(system)?.path shouldBe Path.of("/var/run/konnect/helper.sock")
        }
    }

    context("other addresses") {
        test("keep an explicit socket path and leave its directory alone") {
            val explicit = IpcAddress.UnixSocket(discord)

            linuxWithRuntime.of(explicit) shouldBe SocketLocation(explicit.path, managedDirectory = null)
        }

        test("cannot reach a windows pipe") {
            linuxWithRuntime.of(IpcAddress.WindowsPipe("discord-ipc-0")).shouldBeNull()
        }
    }

    context("locations") {
        test("are equal when their path and directory are") {
            val location = SocketLocation(discord, managedDirectory = null)

            location shouldBe SocketLocation(discord, managedDirectory = null)
            location.hashCode() shouldBe SocketLocation(discord, managedDirectory = null).hashCode()
            location shouldNotBe SocketLocation(discord, managedDirectory = runtime)
            location shouldNotBe SocketLocation(runtime, managedDirectory = null)
            location.equals(discord) shouldBe false
            location.toString() shouldBe "SocketLocation(path=$discord, managedDirectory=none)"
            val managed = SocketLocation(discord, managedDirectory = runtime)
            managed.hashCode() shouldBe SocketLocation(discord, managedDirectory = runtime).hashCode()
        }
    }
})
