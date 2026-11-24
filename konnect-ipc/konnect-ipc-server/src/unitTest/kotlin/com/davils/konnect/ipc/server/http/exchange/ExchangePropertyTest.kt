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

import com.davils.konnect.ipc.server.fixture.hostileText
import com.davils.konnect.ipc.server.http.exchange.fixture.lines
import com.davils.konnect.ipc.server.http.fixture.served
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val CASES: Int = 200
private const val SHORTEST_ECHO: Int = 12
private const val QUICK: Int = 50
private const val PATIENCE_MINUTES: Int = 2
private const val GET_ROOT: String = "GET / HTTP/1.1"
private const val POST_ROOT: String = "POST / HTTP/1.1"
private const val CANARY: String = "zqxjcanary"

private val shapes: List<(String) -> String> = listOf(
    { hostile -> lines("GET /$CANARY$hostile HTTP/1.1", "Host: konnect", "", "") },
    { hostile -> lines("GET /q?$CANARY$hostile HTTP/1.1", "", "") },
    { hostile -> lines(GET_ROOT, "X-Probe: $CANARY$hostile", "", "") },
    { hostile -> lines(GET_ROOT, "$CANARY$hostile: value", "", "") },
    { hostile -> lines("$hostile$CANARY / HTTP/1.1", "", "") },
    { hostile -> lines("GET / HTTP/$CANARY$hostile", "", "") },
    { hostile -> lines(POST_ROOT, "Content-Length: $CANARY$hostile", "", "") },
    { hostile -> lines(POST_ROOT, "Transfer-Encoding: $CANARY$hostile", "", "") },
    { hostile -> lines(POST_ROOT, "Transfer-Encoding: chunked", "", "$CANARY$hostile", "", "") },
    { hostile -> lines(GET_ROOT, "Expect: $CANARY$hostile", "", "") },
)

private val hostileRequests: Arb<Pair<String, String>> = Arb.bind(hostileText, Arb.element(shapes)) { hostile, shape ->
    hostile to shape(hostile)
}

class ExchangePropertyTest : FunSpec({
    context("hostile request lines, headers and bodies") {
        test("are answered or refused with constant text that never quotes them") {
            served(
                configure = {
                    idleTimeout = QUICK.milliseconds
                    requestBodyTimeout = QUICK.milliseconds
                },
                routes = {
                    get("/q") { call.respondText("ok ${call.request.queryParameters.names().size}") }
                    post("/") { call.respondText("ok ${call.receiveText().length}") }
                },
                patience = PATIENCE_MINUTES.minutes,
            ) { running ->
                checkAll(CASES, hostileRequests) { (hostile, request) ->
                    val client = running.connect()

                    client.send(request)
                    val answer = client.rest()

                    answer shouldNotContain CANARY
                    if (hostile.length >= SHORTEST_ECHO) {
                        withClue("an answer must not quote the request") { answer.contains(hostile) shouldBe false }
                    }
                }
            }
        }
    }
})
