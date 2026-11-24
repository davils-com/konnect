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

import com.davils.konnect.ipc.client.Limits
import com.davils.konnect.ipc.client.http.fixture.BODY_CUT_OFF
import com.davils.konnect.ipc.client.http.fixture.InjectingContent
import com.davils.konnect.ipc.client.http.fixture.MALFORMED_HEAD
import com.davils.konnect.ipc.client.http.fixture.PING
import com.davils.konnect.ipc.client.http.fixture.PONG
import com.davils.konnect.ipc.client.http.fixture.STATUS_OK
import com.davils.konnect.ipc.client.http.fixture.answer
import com.davils.konnect.ipc.client.http.fixture.causes
import com.davils.konnect.ipc.client.http.fixture.chunk
import com.davils.konnect.ipc.client.http.fixture.failureOf
import com.davils.konnect.ipc.client.http.fixture.lastChunk
import com.davils.konnect.ipc.client.http.fixture.ok
import com.davils.konnect.ipc.client.http.fixture.wired
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val WAIT: Long = 100L
private const val LENGTH_TWO: String = "Content-Length: 2"
private const val PARTIAL: String = "Content-Length: 10"
private const val INTERIM: String = "100 Continue"
private const val ALLOWED_INTERIM: Int = Limits.MAX_INTERIM_RESPONSES - 1

private suspend fun failureWith(response: String): IOException? = wired { wire ->
    val failure = async { failureOf { wire.client().use { client -> client.get(PING) } } }
    val peer = wire.accept()
    peer.request()
    peer.answer(response)
    failure.await()
}

private fun messagesOf(failure: Throwable?): List<String> {
    val causes = failure?.causes().orEmpty()
    return causes.map { cause -> cause.message.orEmpty() }
}

class IpcHttpLimitsTest : FunSpec({
    context("the head of a response") {
        test("larger than its limit is refused before it is parsed") {
            wired { wire ->
                val failure = async {
                    failureOf {
                        wire.client { maxResponseHeadSize = Limits.MIN_RESPONSE_HEAD_SIZE }.use { client ->
                            client.get(PING)
                        }
                    }
                }
                val peer = wire.accept()
                peer.request()
                val padding = "a".repeat(Limits.MIN_RESPONSE_HEAD_SIZE)
                peer.answer(answer(STATUS_OK, "X-Padding: $padding", "Content-Length: 0"))

                failure.await()?.message shouldBe "The head of the response exceeds its limit"
            }
        }

        test("that ends in the middle is refused") {
            wired { wire ->
                val failure = async { failureOf { wire.client().use { client -> client.get(PING) } } }
                val peer = wire.accept()
                peer.request()
                peer.answer("HTTP/1.1 200 OK\r\nContent-")
                peer.close()

                failure.await()?.message shouldBe
                    "The server closed the connection in the middle of the head of its response"
            }
        }

        test("after too many interim responses is refused") {
            val interim = answer(INTERIM).repeat(Limits.MAX_INTERIM_RESPONSES + 1)

            failureWith(interim + ok)?.message shouldBe "The server sent too many interim responses"
        }

        test("follows every kind of interim response up to the limit") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer("103 Early Hints", "Link: </a>") + answer(INTERIM).repeat(ALLOWED_INTERIM) + ok)

                response.await().bodyAsText() shouldBe PONG
            }
        }

        test("of another version, a status out of range, a broken header or an ambiguous length is refused") {
            listOf(
                ok.replace(oldValue = "HTTP/1.1", newValue = "HTTP/2.0"),
                ok.replace(oldValue = STATUS_OK, newValue = "600 Odd"),
                ok.replace(oldValue = STATUS_OK, newValue = "099 Low"),
                answer(STATUS_OK, LENGTH_TWO, "Content-Length: 3", body = PONG),
                answer(STATUS_OK, "Content-Length: -2", body = PONG),
                answer(STATUS_OK, "Bad Name: x", body = PONG),
                answer(STATUS_OK, LENGTH_TWO, "Transfer-Encoding: chunked", body = chunk(PONG) + lastChunk()),
            ).forEach { response ->
                failureWith(response)?.message shouldBe MALFORMED_HEAD
            }
        }

        test("with a length repeated alike is read") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, LENGTH_TWO, LENGTH_TWO, body = PONG))

                response.await().bodyAsText() shouldBe PONG
            }
        }
    }

    context("the body of a response") {
        test("that ends before its length is refused") {
            wired { wire ->
                val failure = async { failureOf { wire.client().use { client -> client.get(PING) } } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, PARTIAL, body = "abc"))
                peer.close()

                messagesOf(failure.await()) shouldContain BODY_CUT_OFF
            }
        }

        test("that stalls fails with the socket timeout") {
            wired { wire ->
                val failure = async {
                    failureOf {
                        wire.client().use { client ->
                            val response = client.get(PING) { timeout { socketTimeoutMillis = WAIT } }
                            response.bodyAsText()
                        }
                    }
                }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, PARTIAL, body = "abc"))

                val stalled = failure.await()
                val causes = stalled?.causes().orEmpty()
                causes shouldExist { cause -> cause is SocketTimeoutException }
            }
        }

        test("with another transfer coding is read until the server closes") {
            wired { wire ->
                val response = async { wire.client().use { client -> client.get(PING) } }
                val peer = wire.accept()
                peer.request()
                peer.answer(answer(STATUS_OK, "Transfer-Encoding: identity", body = "until close"))
                peer.close()

                response.await().bodyAsText() shouldBe "until close"
            }
        }
    }

    context("the connections of an engine") {
        test("are never more in use than allowed, and a request waits for one no longer than its connect timeout") {
            wired { wire ->
                wire.client { maxConnections = 1 }.use { client ->
                    val started = CompletableDeferred<Unit>()
                    val holding = async {
                        client.prepareGet(PING).execute { response ->
                            started.complete(Unit)
                            response.bodyAsText()
                        }
                    }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(answer(STATUS_OK, LENGTH_TWO))
                    started.await()

                    shouldThrow<ConnectTimeoutException> {
                        client.get(PING) { timeout { connectTimeoutMillis = WAIT } }
                    }
                    val waiting = async { client.get(PING) }
                    delay(WAIT)
                    waiting.cancelAndJoin()
                    wire.connects shouldBe 1

                    peer.answer(PONG)
                    holding.await() shouldBe PONG
                    val third = async { client.get(PING) }
                    peer.request()
                    peer.answer(ok)
                    third.await().bodyAsText() shouldBe PONG
                }
                wire.connects shouldBe 1
            }
        }

        test("are closed once they were idle for longer than allowed") {
            val time = TestTimeSource()
            wired { wire ->
                wire.clientOn(time) { idleTimeout = 1.seconds }.use { client ->
                    val first = async { client.get(PING) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(ok)
                    first.await().bodyAsText() shouldBe PONG

                    val second = async { client.get(PING) }
                    peer.request()
                    peer.answer(ok)
                    second.await().bodyAsText() shouldBe PONG
                    time += 2.seconds

                    val third = async { client.get(PING) }
                    val next = wire.accept()
                    next.request()
                    next.answer(ok)
                    third.await().bodyAsText() shouldBe PONG
                }
                wire.connects shouldBe 2
            }
        }
    }

    context("a header of a request") {
        test("with a line break is refused before anything is sent") {
            wired { wire ->
                val failure = shouldThrow<IllegalArgumentException> {
                    wire.client().use { client -> client.post(PING) { setBody(InjectingContent) } }
                }

                failure.message shouldBe "A header of the request holds a line break"
                wire.connects shouldBe 1
            }
        }

        test("set by the caller is sent") {
            wired { wire ->
                val response = async {
                    wire.client().use { client -> client.get(PING) { header(key = "X-Name", value = "v") } }
                }
                val peer = wire.accept()
                val request = peer.request()
                peer.answer(ok)

                request.headers["x-name"] shouldBe "v"
                response.await().bodyAsText() shouldBe PONG
            }
        }
    }
})
