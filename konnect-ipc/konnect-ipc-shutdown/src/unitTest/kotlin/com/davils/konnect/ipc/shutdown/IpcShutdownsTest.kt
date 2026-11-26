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

package com.davils.konnect.ipc.shutdown

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.shutdown
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.server.IpcServer
import com.davils.konnect.ipc.server.IpcServerState
import com.davils.konnect.ipc.server.http.IpcHttpServer
import com.davils.konnect.ipc.server.http.IpcHttpServerEngine
import com.davils.konnect.ipc.server.ipcServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val SERVER_TASK: String = "ipc-server"
private const val CUSTOM_TASK: String = "control-socket"
private const val ENGINE_TASK: String = "ipc-http-server"

private fun engineOf(ipc: IpcServer): IpcHttpServerEngine {
    val server = embeddedServer(IpcHttpServer, serverConfig { }) { this.server = ipc }
    return server.engine
}

private val threads: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private fun unboundServer(): IpcServer {
    val directory = Files.createTempDirectory("konnect-shutdown")
    return ipcServer(threads) { address = IpcAddress.UnixSocket(directory.resolve("s.sock")) }
}

class IpcShutdownsTest : FunSpec({
    afterSpec { threads.close() }

    context("an IPC server in a shutdown") {
        test("is closed in the stop phase") {
            val server = unboundServer()

            val report = coroutineScope { shutdown(this) { ipcServer(server) }.initiate() }

            server.state.value shouldBe IpcServerState.Closed
            val task = report.tasks.single()
            task.name.toString() shouldBe SERVER_TASK
            task.phase shouldBe ShutdownPhase.Stop
            task.outcome.isCompleted shouldBe true
        }

        test("closes without a failure when the application closed it first") {
            val server = unboundServer()
            server.close()

            val report = coroutineScope { shutdown(this) { ipcServer(server, name = CUSTOM_TASK) }.initiate() }

            report.isClean shouldBe true
            report.tasks
                .single()
                .name
                .toString() shouldBe CUSTOM_TASK
        }
    }

    context("an HTTP engine over IPC in a shutdown") {
        test("is stopped in the drain phase, also when it never ran") {
            val engine = engineOf(unboundServer())

            val report = coroutineScope { shutdown(this) { ipcHttpServer(engine) }.initiate() }

            val task = report.tasks.single()
            task.name.toString() shouldBe ENGINE_TASK
            task.phase shouldBe ShutdownPhase.Drain
            task.outcome.isCompleted shouldBe true
        }

        test("refuses a grace period that is negative or not finite") {
            val engine = engineOf(unboundServer())

            coroutineScope {
                listOf(Duration.INFINITE, (-1).seconds).forEach { grace ->
                    shouldThrow<IllegalArgumentException> {
                        shutdown(this) { ipcHttpServer(engine, gracePeriod = grace) }
                    }
                }
            }
        }
    }
})
