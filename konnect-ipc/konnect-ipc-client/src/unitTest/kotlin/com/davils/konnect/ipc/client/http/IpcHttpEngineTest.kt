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

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.IpcConnector
import com.davils.konnect.ipc.client.http.fixture.PING
import com.davils.konnect.ipc.client.http.fixture.PONG
import com.davils.konnect.ipc.client.http.fixture.STATUS_OK
import com.davils.konnect.ipc.client.http.fixture.answer
import com.davils.konnect.ipc.client.http.fixture.chunk
import com.davils.konnect.ipc.client.http.fixture.failureOf
import com.davils.konnect.ipc.client.http.fixture.lastChunk
import com.davils.konnect.ipc.client.http.fixture.wired
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import java.net.SocketTimeoutException

private const val NO_LENGTH: String = "Content-Length: 0"
private const val WAIT: Long = 50L

private val versioned: String = answer(STATUS_OK, "Content-Length: 2", "Api-Version: 1.56", body = PONG)

private val streamed = object : OutgoingContent.WriteChannelContent() {
    override val contentType: ContentType = ContentType.Application.OctetStream

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeStringUtf8("con")
        channel.writeStringUtf8("text")
    }
}

private fun clientOf(connector: IpcConnector): HttpClient = HttpClient(IpcHttp) {
    engine { this.connector = connector }
}

class IpcHttpEngineTest : FunSpec({
    context("a request") {
        test("is written with its target, its host and no body, and its answer is read with its headers") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                val request = peer.request()
                peer.answer(versioned)

                request.line shouldBe "GET /v1.56/_ping"
                request.headers["host"] shouldBe "docker"
                request.headers.containsKey("content-length") shouldBe false
                response.await().bodyAsText() shouldBe PONG
                response.await().headers["Api-Version"] shouldBe "1.56"
            }
        }

        test("sends a body of known length with its length and its type") {
            wired { wire ->
                val json = TextContent("{}", ContentType.Application.Json)
                val response = async { wire.client().use { client -> client.post(PING) { setBody(json) } } }
                val peer = wire.accept()
                val request = peer.request()
                peer.answer(versioned)

                request.headers["content-length"] shouldBe "2"
                request.headers["content-type"].orEmpty() shouldContain "application/json"
                request.body shouldBe "{}"
                response.await().status shouldBe HttpStatusCode.OK
            }
        }

        test("sends a body of unknown length in chunks") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.post(PING) { setBody(streamed) } } }
                val peer = wire.accept()
                val request = peer.request()
                peer.answer(versioned)

                request.headers["transfer-encoding"] shouldBe "chunked"
                request.body shouldBe "context"
                response.await().bodyAsText() shouldBe PONG
            }
        }
    }

    context("a response") {
        test("is decoded from chunks") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, "Transfer-Encoding: chunked") + chunk("abc") + chunk("de") + lastChunk())

                response.await().bodyAsText() shouldBe "abcde"
            }
        }

        test("without a length is read until the server closes, and its connection is not used again") {
            wired { wire ->
                wire.client().use { client ->
                    val first = async { client.get(PING) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(answer(STATUS_OK, body = "raw stream"))
                    peer.close()
                    first.await().bodyAsText() shouldBe "raw stream"

                    val second = async { client.get(PING) }
                    val next = wire.accept()
                    next.request()
                    next.answer(versioned)
                    second.await().bodyAsText() shouldBe PONG
                }
                wire.connects shouldBe 2
            }
        }

        test("follows a continue before the final answer") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer("100 Continue") + versioned)

                response.await().bodyAsText() shouldBe PONG
            }
        }

        test("fails when the server closes before it answers") {
            wired { wire ->
                val failure = async { failureOf { wire.client().use { client -> client.get(PING) } } }
                val peer = wire.accept()
                peer.request()
                peer.close()

                failure.await()?.message shouldBe "The server closed the connection before it answered"
            }
        }
    }

    context("a connection") {
        test("is kept alive and used again for the next request") {
            wired { wire ->
                wire.client().use { client ->
                    val first = async { client.get(PING) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(versioned)
                    first.await().bodyAsText() shouldBe PONG

                    val second = async { client.head(PING) }
                    peer.request().line shouldBe "HEAD /v1.56/_ping"
                    peer.answer(answer("204 No Content"))
                    second.await().status shouldBe HttpStatusCode.NoContent
                }
                wire.connects shouldBe 1
            }
        }

        test("is closed when the server says so or keeps no idle connection") {
            wired { wire ->
                wire.client(maxIdle = 0).use { client ->
                    val first = async { client.get(PING) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(answer(STATUS_OK, NO_LENGTH, "Connection: close"))
                    first.await().status shouldBe HttpStatusCode.OK

                    val second = async { client.get(PING) }
                    val next = wire.accept()
                    next.request()
                    next.answer(versioned)
                    second.await().bodyAsText() shouldBe PONG
                }
                wire.connects shouldBe 2
            }
        }

        test("that cannot be opened fails with the kind of the result of the connector") {
            val failure = shouldThrow<IpcHttpConnectException> {
                clientOf { IpcConnectResult.NotFound }.use { client -> client.get(PING) }
            }

            failure.result shouldBe IpcConnectResult.NotFound
            failure.message shouldBe "The engine could not connect to the server: not_found"
        }

        test("that takes too long to open fails in time") {
            shouldThrow<ConnectTimeoutException> {
                clientOf { awaitCancellation() }.use { client ->
                    client.get(PING) { timeout { connectTimeoutMillis = WAIT } }
                }
            }
        }

        test("whose server takes too long to answer fails in time") {
            wired { wire ->
                val failure = async {
                    failureOf {
                        wire.client().use { client -> client.get(PING) { timeout { socketTimeoutMillis = WAIT } } }
                    }
                }
                wire.accept().request()

                failure.await().shouldBeInstanceOf<SocketTimeoutException>()
            }
        }
    }
})
