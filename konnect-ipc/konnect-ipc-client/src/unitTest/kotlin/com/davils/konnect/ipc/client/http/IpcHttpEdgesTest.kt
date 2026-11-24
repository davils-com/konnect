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

package com.davils.konnect.ipc.client.http

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.IpcConnector
import com.davils.konnect.ipc.client.http.fixture.FailingAfterWrite
import com.davils.konnect.ipc.client.http.fixture.MALFORMED_BODY
import com.davils.konnect.ipc.client.http.fixture.MALFORMED_HEAD
import com.davils.konnect.ipc.client.http.fixture.PING
import com.davils.konnect.ipc.client.http.fixture.PONG
import com.davils.konnect.ipc.client.http.fixture.STATUS_OK
import com.davils.konnect.ipc.client.http.fixture.answer
import com.davils.konnect.ipc.client.http.fixture.causes
import com.davils.konnect.ipc.client.http.fixture.failureOf
import com.davils.konnect.ipc.client.http.fixture.ok
import com.davils.konnect.ipc.client.http.fixture.wired
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.testing.ipc.TestIpc
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

private const val PATIENCE: Long = 5_000L
private const val SETTLE: Long = 200L
private const val READ_LENGTH: Long = 4L
private const val BUFFER: Int = 1024
private const val CLOSING: String = "Connection: close"

private val readable = object : OutgoingContent.ReadChannelContent() {
    override val contentLength: Long = READ_LENGTH

    override fun readFrom(): ByteReadChannel = ByteReadChannel("read")
}

private val wrapped = object : OutgoingContent.ContentWrapper(TextContent("wrapped", ContentType.Text.Plain)) {
    override fun copy(delegate: OutgoingContent): OutgoingContent.ContentWrapper = this
}

private fun clientOf(connector: IpcConnector): HttpClient = HttpClient(IpcHttp) {
    engine { this.connector = connector }
}

private suspend fun rawOf(client: HttpClient): String {
    val statement = client.preparePost(PING)
    val bytes = statement.execute { response -> response.bodyAsChannel().toByteArray() }
    return bytes.decodeToString()
}

private fun messagesOf(failure: Throwable?): List<String> {
    val causes = failure?.causes().orEmpty()
    return causes.map { cause -> cause.message.orEmpty() }
}

class IpcHttpEdgesTest : FunSpec({
    context("a body") {
        test("read from a channel, wrapped in another one or missing is framed by its length") {
            wired { wire ->
                wire.client().use { client ->
                    val bodies = listOf(readable, wrapped, EmptyContent)
                    val lengths = bodies.map { content ->
                        val response = async { client.post(PING) { setBody(content) } }
                        val peer = wire.accept()
                        val request = peer.request()
                        peer.answer(answer(STATUS_OK, "Content-Length: 0", CLOSING))
                        response.await()
                        request.headers["content-length"] to request.body
                    }

                    lengths shouldBe listOf("4" to "read", "7" to "wrapped", "0" to "")
                }
            }
        }
    }

    context("a response") {
        test("of HTTP 1.0 or without content to a GET is read, and only the latter keeps its connection") {
            wired { wire ->
                wire.client().use { client ->
                    val old = async { client.get(PING) }
                    val first = wire.accept()
                    first.request()
                    first.answer(ok.replace(oldValue = "HTTP/1.1", newValue = "HTTP/1.0"))
                    old.await().bodyAsText() shouldBe PONG

                    val empty = async { client.get(PING) }
                    val second = wire.accept()
                    second.request()
                    second.answer(answer("204 No Content"))
                    empty.await().status shouldBe HttpStatusCode.NoContent
                }
                wire.connects shouldBe 2
            }
        }

        test("that switches the protocol unasked is read as raw bytes") {
            wired { wire ->
                val raw = async { wire.client().use { client -> rawOf(client) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer("101 Switching Protocols", body = "raw"))
                peer.close()

                raw.await() shouldBe "raw"
            }
        }

        test("shows the canonical text of its status, never the text of the server") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer("200 <script>alert(1)</script>", "Content-Length: 0"))

                response.await().status.description shouldBe "OK"
            }
        }
    }

    context("a response that is broken") {
        test("with a length that is no number is refused") {
            wired { wire ->
                val failure = async { failureOf { wire.client().use { client -> client.get(PING) } } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, "Content-Length: many"))

                failure.await()?.message shouldBe MALFORMED_HEAD
            }
        }

        test("in its chunks fails the reader of its body") {
            wired { wire ->
                val failure = async {
                    failureOf { wire.client().use { client -> client.get(PING).bodyAsText() } }
                }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, "Transfer-Encoding: chunked", body = "zz\r\n"))
                peer.close()

                messagesOf(failure.await()) shouldContain MALFORMED_BODY
            }
        }
    }

    context("a timeout") {
        test("that names no time for the answer or one that is not reached lets the answer through") {
            wired { wire ->
                wire.client().use { client ->
                    val first = async { client.get(PING) { timeout { connectTimeoutMillis = PATIENCE } } }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(ok)
                    first.await().bodyAsText() shouldBe PONG

                    val second = async { client.get(PING) { timeout { socketTimeoutMillis = PATIENCE } } }
                    peer.request()
                    peer.answer(ok)
                    second.await().bodyAsText() shouldBe PONG
                }
            }
        }
    }

    context("an idle connection") {
        test("that the server closed is dropped and a new one is opened") {
            wired { wire ->
                wire.client().use { client ->
                    val first = async { client.get(PING) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(ok)
                    first.await().bodyAsText() shouldBe PONG
                    peer.close()
                    delay(SETTLE)

                    val second = async { client.get(PING) }
                    val next = wire.accept()
                    next.request()
                    next.answer(ok)
                    second.await().bodyAsText() shouldBe PONG
                }
                wire.connects shouldBe 2
            }
        }
    }

    context("a connection that fails") {
        test("while the request is written says so in constant text") {
            val closed = IpcConnector {
                val pair = TestIpc.connections()
                pair.client.close()
                IpcConnectResult.Connected(pair.client)
            }

            val failure = failureOf { clientOf(closed).use { client -> client.get(PING) } }

            failure?.message shouldBe "The connection refused a write"
        }

        test("while the answer is read says so in constant text") {
            val broken = IpcConnector { IpcConnectResult.Connected(IpcConnection(FailingAfterWrite(), BUFFER)) }

            val failure = failureOf { clientOf(broken).use { client -> client.get(PING) } }

            messagesOf(failure) shouldContain "The connection failed while it was read"
        }

        test("to open without a connector names that it has none") {
            val failure = shouldThrow<IpcHttpConnectException> {
                HttpClient(IpcHttp).use { client -> client.get(PING) }
            }

            failure.result shouldBe IpcConnectResult.Unavailable("the engine names no connector")
        }
    }
})
