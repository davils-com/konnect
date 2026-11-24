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

import com.davils.konnect.ipc.server.http.exchange.fixture.Greeting
import com.davils.konnect.ipc.server.http.exchange.fixture.broken
import com.davils.konnect.ipc.server.http.exchange.fixture.chunked
import com.davils.konnect.ipc.server.http.exchange.fixture.get
import com.davils.konnect.ipc.server.http.exchange.fixture.lines
import com.davils.konnect.ipc.server.http.exchange.fixture.post
import com.davils.konnect.ipc.server.http.exchange.fixture.request
import com.davils.konnect.ipc.server.http.exchange.fixture.untilClose
import com.davils.konnect.ipc.server.http.fixture.served
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldNotEndWith
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlin.time.Duration.Companion.milliseconds

private const val ECHO: String = "/echo"
private const val CLOSING: String = "/closing"
private const val POST_ECHO: String = "POST $ECHO HTTP/1.1"
private const val CHUNKED: String = "Transfer-Encoding: chunked"
private const val BODY: String = "body"
private const val CLOSE: String = "close"
private const val FOUR_BYTES: String = "Content-Length: 4"
private const val TEN_BYTES: String = "Content-Length: 10"
private const val SMALL_HEAD: Int = 64
private const val FILLER: Int = 100
private const val LONG_LINE: Int = 9_000
private const val SMALL_BODY: Long = 3
private const val IDLE: Int = 50
private const val BODY_TIMEOUT: Int = 100

private fun Routing.edges(heard: CompletableDeferred<String?> = CompletableDeferred()) {
    post(ECHO) { call.respondText(call.receiveText()) }
    post("/ignore") {
        awaitCancellation()
    }
    post("/answer-first") {
        call.respondText("early")
        call.receiveText()
    }
    get("/until-close") {
        call.response.headers.append(name = HttpHeaders.TransferEncoding, value = "identity", safeOnly = false)
        call.respond(untilClose)
    }
    get("/broken") { call.respond(broken) }
    get("/failing") { error("the handler failed") }
    get(CLOSING) {
        call.response.header(name = HttpHeaders.Connection, value = CLOSE)
        call.respondText("bye")
    }
    get("/switch") { call.respond(Greeting(heard)) }
}

class ExchangeEdgesTest : FunSpec({
    context("an expectation") {
        test("of 100-continue is confirmed before the body is read") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request(POST_ECHO, FOUR_BYTES, "Expect: 100-continue"))
                val interim = client.response()
                client.send(BODY)

                interim.status shouldBe HttpStatusCode.Continue.value
                client.response().body shouldBe BODY
            }
        }

        test("of 100-continue without a body is answered without an interim answer") {
            served(routes = { edges() }) { running ->
                running.connect().exchange(get(CLOSING, "Expect: 100-continue")).status shouldBe
                    HttpStatusCode.OK.value
            }
        }

        test("the engine does not know is refused") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                val response = client.exchange(
                    request(POST_ECHO, FOUR_BYTES, "Expect: something-else", body = BODY),
                )

                response.status shouldBe HttpStatusCode.ExpectationFailed.value
                response.headers shouldContain ("connection" to CLOSE)
                client.rest() shouldBe ""
            }
        }
    }

    context("a malformed request") {
        test("is answered with bad request and closes the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.exchange(lines("NOT A REQUEST LINE AT ALL", "", "")).status shouldBe
                    HttpStatusCode.BadRequest.value
                client.rest() shouldBe ""
            }
        }

        test("with an invalid length, with two framings or an unknown encoding is refused") {
            served(routes = { edges() }) { running ->
                val invalid = request(POST_ECHO, "Content-Length: many")
                val signed = request(POST_ECHO, "Content-Length: +4", body = BODY)
                val twice = request(POST_ECHO, FOUR_BYTES, CHUNKED)
                val gzip = request(POST_ECHO, "Transfer-Encoding: gzip")
                val doubled = request(POST_ECHO, CHUNKED, CHUNKED)

                running.connect().exchange(invalid).status shouldBe HttpStatusCode.BadRequest.value
                running.connect().exchange(signed).status shouldBe HttpStatusCode.BadRequest.value
                running.connect().exchange(twice).status shouldBe HttpStatusCode.BadRequest.value
                running.connect().exchange(gzip).status shouldBe HttpStatusCode.NotImplemented.value
                running.connect().exchange(doubled).status shouldBe HttpStatusCode.NotImplemented.value
            }
        }

        test("that ends before its head is complete closes the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(lines("GET $ECHO HTTP/1.1", "Host: ko"))
                client.close()

                client.rest() shouldBe ""
            }
        }
    }

    context("a limit") {
        test("on the head answers a head that is too large") {
            served(configure = { maxHeaderSize = SMALL_HEAD }, routes = { edges() }) { running ->
                val client = running.connect()

                client.exchange(get(ECHO, "X-Filler: " + "a".repeat(FILLER))).status shouldBe
                    HttpStatusCode.RequestHeaderFieldTooLarge.value
                client.rest() shouldBe ""
            }
        }

        test("on a line answers a header line the parser refuses") {
            served(routes = { edges() }) { running ->
                running.connect().exchange(get(ECHO, "X-Filler: " + "a".repeat(LONG_LINE))).status shouldBe
                    HttpStatusCode.RequestHeaderFieldTooLarge.value
            }
        }

        test("on the body answers a declared length that is too large") {
            served(configure = { maxRequestBodySize = SMALL_BODY }, routes = { edges() }) { running ->
                val response = running.connect().exchange(post(path = ECHO, body = BODY))

                response.status shouldBe HttpStatusCode.PayloadTooLarge.value
            }
        }

        test("on the body answers a chunked body that grows too large") {
            served(configure = { maxRequestBodySize = SMALL_BODY }, routes = { edges() }) { running ->
                val client = running.connect()

                client.exchange(request(POST_ECHO, CHUNKED, body = chunked("bo", "dy"))).status shouldBe
                    HttpStatusCode.PayloadTooLarge.value
                client.rest() shouldBe ""
            }
        }

        test("on idleness closes a connection that sends nothing") {
            served(configure = { idleTimeout = IDLE.milliseconds }, routes = { edges() }) { running ->
                running.connect().rest() shouldBe ""
            }
        }

        test("on the time of a body answers a body that does not arrive in time and closes the connection") {
            served(configure = { requestBodyTimeout = BODY_TIMEOUT.milliseconds }, routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request(POST_ECHO, TEN_BYTES, body = "slow"))

                val response = client.response()
                response.status shouldBe HttpStatusCode.RequestTimeout.value
                response.headers shouldContain ("connection" to CLOSE)
                client.rest() shouldBe ""
            }
        }

        test("on the time of a body closes the connection of an answer that was sent already") {
            served(configure = { requestBodyTimeout = BODY_TIMEOUT.milliseconds }, routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request("POST /answer-first HTTP/1.1", TEN_BYTES, body = "slow"))

                client.response().body shouldBe "early"
                client.rest() shouldBe ""
            }
        }

        test("on the time of a body also runs while the application does not answer") {
            served(configure = { requestBodyTimeout = BODY_TIMEOUT.milliseconds }, routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request("POST /ignore HTTP/1.1", TEN_BYTES, body = "slow"))
                client.close()

                client.rest() shouldBe ""
            }
        }
    }

    context("a body") {
        test("that ends early closes the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request(POST_ECHO, TEN_BYTES, body = "short"))
                client.closeOutput()

                client.rest() shouldBe ""
            }
        }

        test("with broken chunks is refused with constant text and closes the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(request(POST_ECHO, CHUNKED, body = lines("zz-secret", BODY, "")))

                val response = client.response()
                response.status shouldBe HttpStatusCode.BadRequest.value
                response.body shouldBe "The body of the request is malformed or ended early"
                response.body shouldNotContain "secret"
                client.rest() shouldBe ""
            }
        }
    }

    context("an answer") {
        test("without a length or chunks is ended by closing the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(get("/until-close"))
                val answer = client.rest()

                answer shouldContain "Connection: close"
                answer shouldEndWith lines("", "", "until close")
            }
        }

        test("that the application breaks while it streams closes the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.send(get("/broken"))

                client.rest() shouldNotEndWith lines("0", "", "")
            }
        }

        test("of a failing handler is a server error and keeps the connection") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.exchange(get("/failing")).status shouldBe HttpStatusCode.InternalServerError.value
                client.exchange(post(path = ECHO, body = "still")).body shouldBe "still"
            }
        }

        test("that asks to close the connection closes it") {
            served(routes = { edges() }) { running ->
                val client = running.connect()

                client.exchange(get(CLOSING)).body shouldBe "bye"
                client.rest() shouldBe ""
            }
        }
    }

    context("an upgrade") {
        test("switches the connection to the protocol of the application and closes it afterwards") {
            val heard = CompletableDeferred<String?>()
            served(routes = { edges(heard) }) { running ->
                val client = running.connect()

                val switched = client.exchange(
                    get("/switch", "Connection: Upgrade", "Upgrade: greeting"),
                    isHead = true,
                )
                client.send("hello\n")

                switched.status shouldBe HttpStatusCode.SwitchingProtocols.value
                switched.headers shouldContain ("upgrade" to "greeting")
                client.rest() shouldBe "welcome\n"
                heard.await() shouldBe "hello"
            }
        }
    }
})
