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

package com.davils.konnect.ipc.client.http

import com.davils.konnect.ipc.client.fixture.isUnix
import com.davils.konnect.ipc.client.fixture.socketPath
import com.davils.konnect.ipc.client.ipcClient
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.core.address.IpcAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.server.cio.CIO
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

private const val PING: String = "/v1.56/_ping"
private const val ECHO: String = "/echo"
private const val PONG: String = "OK"

private val dockerSocket: Path = Path.of("/var/run/docker.sock")

private val hasDocker: Boolean = isUnix && Files.exists(dockerSocket) && Files.isWritable(dockerSocket)

private val streamed = object : OutgoingContent.WriteChannelContent() {
    override val contentType: ContentType = ContentType.Text.Plain

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeStringUtf8("streamed ")
        channel.writeStringUtf8("body")
    }
}

private fun clientOf(dispatcher: CoroutineDispatcher, path: Path, vararg trusted: IpcUser): HttpClient =
    HttpClient(IpcHttp) {
        followRedirects = false
        engine {
            connector = ipcClient(dispatcher) {
                address = IpcAddress.UnixSocket(path)
                trust { trusted.forEach(::allow) }
            }
        }
    }

class IpcHttpSocketTest : FunSpec({
    val threads: ExecutorCoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    val stops = mutableListOf<() -> Unit>()

    afterSpec {
        stops.forEach { stop -> stop() }
        threads.close()
    }

    context("a client over a unix domain socket").config(enabledIf = { isUnix }) {
        val path = socketPath()
        val server = embeddedServer(CIO, configure = { unixConnector(path.toString()) }) {
            routing {
                get(PING) { call.respondText(PONG) }
                post(ECHO) { call.respondText(call.receiveText()) }
                get("/lines") {
                    call.respondTextWriter {
                        write("first\n")
                        flush()
                        write("second\n")
                    }
                }
            }
        }
        server.start(wait = false)
        stops += { server.stop() }

        test("asks a real server and keeps its connection for the next request") {
            clientOf(threads, path).use { client ->
                client.get(PING).bodyAsText() shouldBe PONG
                client.get(PING).bodyAsText() shouldBe PONG
            }
        }

        test("sends a body of known and of unknown length") {
            clientOf(threads, path).use { client ->
                client.post(ECHO) { setBody("known") }.bodyAsText() shouldBe "known"
                client.post(ECHO) { setBody(streamed) }.bodyAsText() shouldBe "streamed body"
            }
        }

        test("reads an answer the server writes in chunks") {
            clientOf(threads, path).use { client ->
                val response = client.get("/lines")

                response.headers[HttpHeaders.TransferEncoding] shouldBe "chunked"
                response.bodyAsText() shouldBe "first\nsecond\n"
            }
        }
    }

    context("the Docker Engine of this machine").config(enabledIf = { hasDocker }) {
        test("answers a ping over its socket") {
            clientOf(threads, dockerSocket, IpcUser.UnixUser.ROOT).use { client ->
                client.get("http://docker/_ping").bodyAsText() shouldBe PONG
            }
        }
    }
})
