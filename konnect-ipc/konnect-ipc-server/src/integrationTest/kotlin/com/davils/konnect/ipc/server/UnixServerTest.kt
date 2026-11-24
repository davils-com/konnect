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

package com.davils.konnect.ipc.server

import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.access.ipcAccess
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.connection.IpcReadResult
import com.davils.konnect.ipc.server.fixture.isUnix
import com.davils.konnect.ipc.server.fixture.socketAddress
import com.davils.konnect.ipc.server.fixture.withThreads
import com.davils.konnect.ipc.server.outcome.IpcAcceptResult
import com.davils.konnect.ipc.server.outcome.IpcBindResult
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.bytestring.ByteString
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration.Companion.milliseconds

private const val WAIT: Int = 100
private const val BINDS: Int = 20
private val message: ByteArray = byteArrayOf(1, 2, 3)
private val owner: Set<PosixFilePermission> = PosixFilePermissions.fromString("rw-------")

private fun serverAt(
    address: IpcAddress,
    dispatcher: CoroutineDispatcher,
    access: IpcAccess = IpcAccess.CURRENT_USER,
): IpcServer = ipcServer(dispatcher) {
    this.address = address
    this.access = access
}

private fun Path.staged(): List<Path> = parent.listDirectoryEntries(".konnect-*")

private fun permissionsOrNull(path: Path): Set<PosixFilePermission>? = try {
    Files.getPosixFilePermissions(path)
} catch (expectedMissing: NoSuchFileException) {
    null
}

@OptIn(ExperimentalAtomicApi::class)
private suspend fun watchedWhileBound(path: Path, dispatcher: CoroutineDispatcher): Set<Set<PosixFilePermission>> {
    val seen: MutableSet<Set<PosixFilePermission>> = ConcurrentHashMap.newKeySet()
    val watching = AtomicBoolean(true)
    val watcher = Thread {
        while (watching.load()) permissionsOrNull(path)?.let(seen::add)
    }
    watcher.start()
    repeat(BINDS) {
        val server = serverAt(IpcAddress.UnixSocket(path), dispatcher)
        server.bind() shouldBe IpcBindResult.Bound
        server.close()
    }
    watching.store(false)
    watcher.join()
    return seen
}

class UnixServerTest : FunSpec({
    context("a server on a unix domain socket").config(enabledIf = { isUnix }) {
        test("may only be opened by its owner and removes its socket and staging directory when closed") {
            val address = socketAddress()

            withThreads { dispatcher ->
                val server = serverAt(address, dispatcher)
                server.bind() shouldBe IpcBindResult.Bound

                Files.getPosixFilePermissions(address.path) shouldBe owner
                address.path.staged().shouldBeEmpty()
                server.close()
            }

            Files.exists(address.path) shouldBe false
            address.path.staged().shouldBeEmpty()
        }

        test("never shows its path with more rights than configured") {
            val address = socketAddress()

            val seen = withThreads { dispatcher -> watchedWhileBound(address.path, dispatcher) }

            seen.shouldNotBeEmpty()
            seen shouldBe setOf(owner)
        }

        test("opens its socket to every user when other users may connect") {
            val address = socketAddress()

            withThreads { dispatcher ->
                val server = serverAt(address, dispatcher, ipcAccess { allow(IpcUser.UnixUser.ROOT) })
                server.bind() shouldBe IpcBindResult.Bound

                Files.getPosixFilePermissions(address.path) shouldBe PosixFilePermissions.fromString("rw-rw-rw-")
                server.close()
            }
        }

        test("accepts a client, knows its user and reads what it wrote") {
            val address = socketAddress()

            withThreads { dispatcher ->
                val server = serverAt(address, dispatcher)
                server.bind()
                coroutineScope {
                    val accepting = async { server.accept() }
                    val client = SocketChannel.open(UnixDomainSocketAddress.of(address.path))
                    client.write(ByteBuffer.wrap(message))
                    val accepted = accepting.await().shouldBeInstanceOf<IpcAcceptResult.Accepted>()

                    accepted.connection.peer.user shouldBe IpcUser.UnixUser(IpcPlatform.environment.userName)
                    accepted.connection.readExactly(message.size) shouldBe IpcReadResult.Received(ByteString(message))
                    client.close()
                    accepted.connection.close()
                }
                server.close()
            }
        }

        test("refuses a second server and takes over a socket left behind") {
            val address = socketAddress()
            val stale = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
            stale.bind(UnixDomainSocketAddress.of(address.path))
            stale.close()

            withThreads { dispatcher ->
                val first = serverAt(address, dispatcher)

                first.bind() shouldBe IpcBindResult.Bound
                serverAt(address, dispatcher).bind() shouldBe IpcBindResult.AddressInUse
                address.path.staged().shouldBeEmpty()
                first.close()
            }
        }

        test("keeps listening after a cancelled accept") {
            val address = socketAddress()

            withThreads { dispatcher ->
                val server = serverAt(address, dispatcher)
                server.bind()

                withTimeoutOrNull(WAIT.milliseconds) { server.accept() }

                coroutineScope {
                    val accepting = async { server.accept() }
                    val client = SocketChannel.open(UnixDomainSocketAddress.of(address.path))
                    accepting.await().shouldBeInstanceOf<IpcAcceptResult.Accepted>()
                    client.close()
                }
                server.close()
            }
        }
    }
})
