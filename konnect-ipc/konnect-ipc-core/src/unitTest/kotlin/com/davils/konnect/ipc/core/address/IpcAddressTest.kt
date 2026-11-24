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

package com.davils.konnect.ipc.core.address

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.fixture.HELPER_NAME
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Path

private const val TOO_LONG_NAME: Int = Limits.MAX_NAME_LENGTH + 1
private const val TOO_LONG_PIPE: Int = Limits.MAX_PIPE_NAME_LENGTH + 1
private const val DISCORD: String = "discord-ipc-0"
private const val SOCKET: String = "/run/konnect.sock"

class IpcAddressTest : FunSpec({
    context("names") {
        test("accept lower case letters, digits, dots, dashes and underscores") {
            IpcName("konnect.helper-1_a").value shouldBe "konnect.helper-1_a"
            IpcName("a".repeat(Limits.MAX_NAME_LENGTH)).value.length shouldBe Limits.MAX_NAME_LENGTH
        }

        test("refuse upper case, separators and names that are too long") {
            listOf("Helper", "a/b", "-start", "", "a".repeat(TOO_LONG_NAME)).forEach { text ->
                IpcName.parseOrNull(text).shouldBeNull()
                shouldThrow<IllegalArgumentException> { IpcName(text) }
            }
        }

        test("read as their value") {
            IpcName.parseOrNull(HELPER_NAME)?.toString() shouldBe HELPER_NAME
        }
    }

    context("named addresses") {
        test("belong to the current user unless told otherwise") {
            IpcAddress.Named(IpcName(HELPER_NAME)).scope shouldBe IpcScope.User
        }

        test("read with their scope") {
            IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.System).toString() shouldBe "system:helper"
        }

        test("are equal when their name and scope are") {
            val user = IpcAddress.Named(IpcName(HELPER_NAME))

            user shouldBe IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.User)
            user.hashCode() shouldBe IpcAddress.Named(IpcName(HELPER_NAME)).hashCode()
            user shouldNotBe IpcAddress.Named(IpcName(HELPER_NAME), IpcScope.System)
            user shouldNotBe IpcAddress.Named(IpcName("other"))
            user.equals(HELPER_NAME) shouldBe false
        }
    }

    context("windows pipes") {
        test("take any printable name without a backslash") {
            IpcAddress.WindowsPipe(DISCORD).toString() shouldBe "pipe:discord-ipc-0"
            IpcAddress.WindowsPipe(DISCORD) shouldBe IpcAddress.WindowsPipe(DISCORD)
            IpcAddress.WindowsPipe(DISCORD).hashCode() shouldBe DISCORD.hashCode()
            IpcAddress.WindowsPipe(DISCORD).equals(DISCORD) shouldBe false
        }

        test("refuse empty names, control characters, backslashes and names that are too long") {
            listOf("", "a\u0000b", "a\\b", "a".repeat(TOO_LONG_PIPE)).forEach { name ->
                IpcAddress.WindowsPipe.isValid(name) shouldBe false
                shouldThrow<IllegalArgumentException> { IpcAddress.WindowsPipe(name) }
            }
        }
    }

    context("unix sockets") {
        test("take an absolute path") {
            val socket = IpcAddress.UnixSocket(Path.of(SOCKET))

            socket.toString() shouldBe "unix:/run/konnect.sock"
            socket shouldBe IpcAddress.UnixSocket(Path.of(SOCKET))
            socket.hashCode() shouldBe Path.of(SOCKET).hashCode()
            socket.equals(SOCKET) shouldBe false
        }

        test("refuse relative paths, control characters and paths longer than a socket address holds") {
            val tooLong = Path.of("/" + "a".repeat(Limits.MAX_SOCKET_PATH_BYTES))
            val tooManyBytes = Path.of("/" + "ä".repeat(Limits.MAX_SOCKET_PATH_BYTES / 2 + 1))
            listOf(Path.of("konnect.sock"), Path.of("/run/a\nb.sock"), tooLong, tooManyBytes).forEach { path ->
                IpcAddress.UnixSocket.isValid(path) shouldBe false
                shouldThrow<IllegalArgumentException> { IpcAddress.UnixSocket(path) }
            }
        }
    }
})
