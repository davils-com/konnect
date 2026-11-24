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

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.access.ipcAccess
import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.konnect.ipc.core.transport.unix.SocketLocation
import com.davils.konnect.ipc.core.transport.unix.UnixSocketApi
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.fixture.refusal
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.konnect.ipc.server.transport.ListenOutcome
import com.davils.konnect.testing.ipc.FakeUnixSocket
import com.davils.konnect.testing.ipc.FakeUnixSocketApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.net.BindException
import java.net.ConnectException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

private const val TOKEN: String = "0badc0de"
private const val MALLORY: String = "mallory"
private const val LONG_DIRECTORY: Int = 90
private val directory: Path = Path.of("/tmp/konnect-alice")
private val socketPath: Path = directory.resolve("helper.sock")
private val stagingDirectory: Path = directory.resolve(".konnect-$TOKEN")
private val stagingSocket: Path = stagingDirectory.resolve("s")
private val managed = SocketLocation(socketPath, managedDirectory = directory)
private val explicit = SocketLocation(socketPath, managedDirectory = null)
private val shared: IpcAccess = ipcAccess { allow(IpcUser.UnixUser.ROOT) }
private val private = PosixFilePermissions.fromString("rwx------")

private fun bind(
    api: UnixSocketApi,
    location: SocketLocation = managed,
    access: IpcAccess = IpcAccess.CURRENT_USER,
): ListenOutcome = SocketBinder(
    api = api,
    userName = "alice",
    dispatcher = immediate(),
    tokens = { TOKEN },
).bind(location, access)

class SocketBinderTest : FunSpec({
    context("binding for the current user only") {
        test("keeps the directory, the staging directory and the socket private") {
            val api = FakeUnixSocketApi()

            bind(api).shouldBeInstanceOf<ListenOutcome.Listening>()

            api.permissions[directory] shouldBe private
            api.permissions[stagingDirectory] shouldBe private
            api.permissions[stagingSocket] shouldBe PosixFilePermissions.fromString("rw-------")
        }
    }

    context("binding for other users") {
        test("opens the socket and leaves the check to the server") {
            val api = FakeUnixSocketApi()

            bind(api, access = shared).shouldBeInstanceOf<ListenOutcome.Listening>()

            api.permissions[directory] shouldBe PosixFilePermissions.fromString("rwx--x--x")
            api.permissions[stagingDirectory] shouldBe private
            api.permissions[stagingSocket] shouldBe PosixFilePermissions.fromString("rw-rw-rw-")
        }
    }

    context("staging") {
        test("binds and restricts the socket in its private directory before it moves it to its path") {
            val api = RecordingUnixSocketApi()

            bind(api).shouldBeInstanceOf<ListenOutcome.Listening>()

            api.operations shouldBe listOf(
                "restrict $directory",
                "restrict $stagingDirectory",
                "bind $stagingSocket",
                "restrict $stagingSocket",
                "move $stagingSocket $socketPath",
            )
        }

        test("leaves the socket at its path and removes the staging directory") {
            val api = FakeUnixSocketApi()

            bind(api)

            api.moved shouldBe listOf(stagingSocket to socketPath)
            api.exists(socketPath) shouldBe true
            api.exists(stagingDirectory) shouldBe false
            api.exists(stagingSocket) shouldBe false
        }

        test("is named by a secure random token of eight hexadecimal digits") {
            val api = FakeUnixSocketApi()

            SocketBinder(api, userName = "alice", dispatcher = immediate()).bind(managed, IpcAccess.CURRENT_USER)

            val staged = api.moved.single().first
            staged.parent.fileName.toString() shouldMatch Regex("\\.konnect-[0-9a-f]{8}")
        }

        test("that exists already is never taken over or removed") {
            val api = FakeUnixSocketApi(existing = setOf(stagingDirectory))

            bind(api).refusal() shouldBe IpcBindResult.AccessDenied

            api.deleted shouldNotContain stagingDirectory
        }

        test("that belongs to somebody else once it was created is refused and left alone") {
            val api = RecordingUnixSocketApi(owners = mapOf(stagingDirectory to MALLORY))

            bind(api, location = explicit).refusal() shouldBe IpcBindResult.AccessDenied

            api.fake.deleted shouldNotContain stagingDirectory
        }

        test("that would not fit the address of a socket is refused") {
            val parent = Path.of("/" + "d".repeat(LONG_DIRECTORY))

            val outcome = bind(
                FakeUnixSocketApi(),
                location = SocketLocation(path = parent.resolve("h.sock"), managedDirectory = null),
            )

            outcome.refusal().shouldBeInstanceOf<IpcBindResult.Unavailable>()
        }

        test("closes the server and cleans up when the socket cannot be restricted") {
            val api = RecordingUnixSocketApi(restrictFailures = setOf(stagingSocket))

            bind(api) shouldBe ListenOutcome.Refused(IpcBindResult.Failed(IpcSystemError(null, "IOException")))

            api.fake.server.closes shouldBe 1
            api.fake.deleted shouldBe listOf(socketPath, stagingSocket, stagingDirectory)
            api.operations shouldNotContain "move $stagingSocket $socketPath"
        }

        test("closes the server and cleans up when the socket cannot be moved") {
            val api = FakeUnixSocketApi(moveFailure = IOException("cross device"))

            val refusal = bind(api).refusal()

            refusal.shouldBeInstanceOf<IpcBindResult.Failed>()

            api.server.closes shouldBe 1
            api.deleted shouldContain stagingDirectory
            api.exists(socketPath) shouldBe false
        }

        test("refuses an address another server took while the socket was staged") {
            val api = RecordingUnixSocketApi(onBind = { fake -> fake.createDirectories(socketPath) })

            bind(api).refusal() shouldBe IpcBindResult.AddressInUse

            api.fake.server.closes shouldBe 1
            api.fake.moved shouldBe emptyList()
        }
    }

    context("the directory") {
        test("that cannot be created or belongs to somebody else is refused") {
            bind(FakeUnixSocketApi(directoryFailure = IOException("denied"))).refusal() shouldBe
                IpcBindResult.AccessDenied
            bind(FakeUnixSocketApi(owner = MALLORY)).refusal() shouldBe IpcBindResult.AccessDenied
        }

        test("of an explicit path is left alone") {
            val api = RecordingUnixSocketApi(owners = mapOf(directory to MALLORY))

            bind(api, explicit).shouldBeInstanceOf<ListenOutcome.Listening>()

            api.operations shouldNotContain "restrict $directory"
        }
    }

    context("an existing socket") {
        test("that answers is in use") {
            bind(FakeUnixSocketApi(existing = setOf(socketPath))).refusal() shouldBe IpcBindResult.AddressInUse
        }

        test("that nobody answers is replaced") {
            val api = FakeUnixSocketApi(
                existing = setOf(socketPath),
                connecting = { throw ConnectException("refused") },
            )

            bind(api).shouldBeInstanceOf<ListenOutcome.Listening>()

            api.deleted shouldContain socketPath
        }

        test("that cannot be probed is kept") {
            val api = FakeUnixSocketApi(existing = setOf(socketPath), connecting = { throw IOException("denied") })

            bind(api).refusal() shouldBe IpcBindResult.AddressInUse
        }

        test("that answers has its probe closed") {
            val probe = FakeUnixSocket()

            bind(FakeUnixSocketApi(existing = setOf(socketPath), connecting = { probe }))

            probe.closes shouldBe 1
        }
    }

    context("a failing bind") {
        test("reports an address in use and any other problem without its message") {
            bind(FakeUnixSocketApi(bindFailure = BindException("in use"))).refusal() shouldBe IpcBindResult.AddressInUse
            bind(FakeUnixSocketApi(bindFailure = IOException("broken /etc/passwd"))).refusal() shouldBe
                IpcBindResult.Failed(IpcSystemError(code = null, description = "IOException"))
        }
    }
})
