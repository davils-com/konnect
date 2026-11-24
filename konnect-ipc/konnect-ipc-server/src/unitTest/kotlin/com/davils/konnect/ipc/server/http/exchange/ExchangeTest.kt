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

package com.davils.konnect.ipc.server.http.exchange

import com.davils.konnect.ipc.server.http.exchange.fixture.chunked
import com.davils.konnect.ipc.server.http.exchange.fixture.get
import com.davils.konnect.ipc.server.http.exchange.fixture.post
import com.davils.konnect.ipc.server.http.exchange.fixture.request
import com.davils.konnect.ipc.server.http.fixture.served
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post

private const val HELLO: String = "/hello"
private const val ECHO: String = "/echo"
private const val LINES: String = "/lines"
private const val GREETING: String = "hello"
private const val CONNECTION: String = "connection"
private const val CLOSE_TEXT: String = "close"
private val CLOSE: Pair<String, String> = CONNECTION to CLOSE_TEXT

private fun Routing.sample() {
    get(HELLO) { call.respondText(GREETING) }
    head(HELLO) { call.respondText(GREETING) }
    post(ECHO) { call.respondText(call.receiveText()) }
    head(LINES) { call.respondTextWriter { write("unseen") } }
    get(LINES) {
        call.respondTextWriter {
            write("first\n")
            flush()
            write("second\n")
        }
    }
}

class ExchangeTest : FunSpec({
    context("a request") {
        test("is routed through the application and answered with a body of known length") {
            served(routes = { sample() }) { running ->
                val response = running.connect().exchange(get(HELLO, "Host: konnect"))

                response.status shouldBe HttpStatusCode.OK.value
                response.headers shouldContain ("content-length" to "5")
                response.body shouldBe GREETING
            }
        }

        test("hands a body of known length to the application") {
            served(routes = { sample() }) { running ->
                running.connect().exchange(post(path = ECHO, body = "known")).body shouldBe "known"
            }
        }

        test("hands a chunked body to the application") {
            served(routes = { sample() }) { running ->
                val response = running.connect().exchange(
                    request("POST $ECHO HTTP/1.1", "Transfer-Encoding: chunked", body = chunked("stre", "amed")),
                )

                response.body shouldBe "streamed"
            }
        }

        test("hands an empty body to the application") {
            served(routes = { sample() }) { running ->
                running.connect().exchange(post(path = ECHO, body = "")).body shouldBe ""
            }
        }

        test("is answered in chunks when the application streams") {
            served(routes = { sample() }) { running ->
                val response = running.connect().exchange(get(LINES))

                response.headers shouldContain ("transfer-encoding" to "chunked")
                response.body shouldBe "first\nsecond\n"
            }
        }

        test("for HEAD is answered with the head alone") {
            served(routes = { sample() }) { running ->
                val client = running.connect()

                val head = client.exchange(request("HEAD $HELLO HTTP/1.1"), isHead = true)
                val streamed = client.exchange(request("HEAD $LINES HTTP/1.1"), isHead = true)
                val next = client.exchange(get(HELLO))

                head.headers shouldContain ("content-length" to "5")
                head.body shouldBe ""
                streamed.headers shouldContain ("transfer-encoding" to "chunked")
                next.body shouldBe GREETING
            }
        }
    }

    context("a connection") {
        test("is kept alive for the next request") {
            served(routes = { sample() }) { running ->
                val client = running.connect()

                client.exchange(get(HELLO)).body shouldBe GREETING
                client.exchange(post(path = ECHO, body = "again")).body shouldBe "again"
            }
        }

        test("answers pipelined requests in order") {
            served(routes = { sample() }) { running ->
                val client = running.connect()

                client.send(get(HELLO) + post(path = ECHO, body = "second") + get(LINES))

                client.response().body shouldBe GREETING
                client.response().body shouldBe "second"
                client.response().body shouldBe "first\nsecond\n"
            }
        }

        test("is closed after a request that asks for it") {
            served(routes = { sample() }) { running ->
                val client = running.connect()

                client.exchange(get(HELLO, "Connection: close")).headers shouldContain CLOSE
                client.rest() shouldBe ""
            }
        }

        test("is closed after a request of HTTP/1.0 unless it asks to keep it") {
            served(routes = { sample() }) { running ->
                val kept = running.connect()
                val closed = running.connect()

                kept.exchange(request("GET $HELLO HTTP/1.0", "Connection: keep-alive")).body shouldBe GREETING
                kept.exchange(get(HELLO)).headers shouldNotContainKey CONNECTION
                closed.exchange(request("GET $HELLO HTTP/1.0")).headers shouldContain CLOSE
                closed.rest() shouldBe ""
            }
        }

        test("serves at most the requests it is allowed to and closes with the last of them") {
            served(configure = { maxRequestsPerConnection = 2 }, routes = { sample() }) { running ->
                val client = running.connect()

                client.send(get(HELLO) + get(HELLO) + get(HELLO))

                client.response().headers shouldNotContainKey CONNECTION
                client.response().headers shouldContain CLOSE
                client.rest() shouldBe ""
            }
        }
    }
})
