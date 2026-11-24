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
import com.davils.konnect.testing.ipc.FakeIpcEnvironment
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Path

private const val RUNTIME: String = "/run/user/1000"
private const val RUNTIME_VARIABLE: String = "XDG_RUNTIME_DIR"
private const val DISCORD: String = "/run/user/1000/discord-ipc-0"
private val linuxWithRuntime = SocketLocations(
    FakeIpcEnvironment(variables = mapOf(RUNTIME_VARIABLE to RUNTIME)),
    isMacOs = false,
)
private val macOs = SocketLocations(
    FakeIpcEnvironment(
        temporaryDirectory = Path.of("/var/folders/x"),
        variables = mapOf(RUNTIME_VARIABLE to RUNTIME),
    ),
    isMacOs = true,
)
private val system = IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.System)

class SocketLocationsTest : FunSpec({
    context("a named address of the user") {
        test("lives in the runtime directory on linux") {
            linuxWithRuntime.of(HELPER) shouldBe SocketLocation(
                path = Path.of("$RUNTIME/konnect/helper.sock"),
                managedDirectory = Path.of("$RUNTIME/konnect"),
            )
        }

        test("lives in a private temporary directory on linux without a runtime directory") {
            SocketLocations(FakeIpcEnvironment(), isMacOs = false).of(HELPER)?.path shouldBe
                Path.of("/tmp/konnect-alice/helper.sock")
        }

        test("lives in the temporary directory of the user on macos") {
            macOs.of(HELPER)?.path shouldBe Path.of("/var/folders/x/konnect/helper.sock")
        }

        test("is unreachable when its path does not fit a socket address") {
            val deep = FakeIpcEnvironment(
                temporaryDirectory = Path.of("/" + "d".repeat(Limits.MAX_SOCKET_PATH_BYTES)),
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
        test("lives in the runtime directory of the system") {
            linuxWithRuntime.of(system)?.path shouldBe Path.of("/run/konnect/helper.sock")
            macOs.of(system)?.path shouldBe Path.of("/var/run/konnect/helper.sock")
        }
    }

    context("other addresses") {
        test("keep an explicit socket path and leave its directory alone") {
            val explicit = IpcAddress.UnixSocket(Path.of(DISCORD))

            linuxWithRuntime.of(explicit) shouldBe SocketLocation(explicit.path, managedDirectory = null)
        }

        test("cannot reach a windows pipe") {
            linuxWithRuntime.of(IpcAddress.WindowsPipe("discord-ipc-0")).shouldBeNull()
        }
    }

    context("locations") {
        test("are equal when their path and directory are") {
            val location = SocketLocation(Path.of(DISCORD), managedDirectory = null)

            location shouldBe SocketLocation(Path.of(DISCORD), managedDirectory = null)
            location.hashCode() shouldBe SocketLocation(Path.of(DISCORD), managedDirectory = null).hashCode()
            location shouldNotBe SocketLocation(Path.of(DISCORD), managedDirectory = Path.of(RUNTIME))
            location shouldNotBe SocketLocation(Path.of(RUNTIME), managedDirectory = null)
            location.equals(DISCORD) shouldBe false
            location.toString() shouldBe "SocketLocation(path=$DISCORD, managedDirectory=none)"
            val managed = SocketLocation(Path.of(DISCORD), managedDirectory = Path.of(RUNTIME))
            managed.hashCode() shouldBe SocketLocation(Path.of(DISCORD), managedDirectory = Path.of(RUNTIME)).hashCode()
        }
    }
})
