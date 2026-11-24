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

package com.davils.konnect.ipc.server.http.call

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.http.IpcHttpCallAttributes
import com.davils.konnect.ipc.server.http.exchange.fixture.get
import com.davils.konnect.ipc.server.http.fixture.served
import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

private fun Routing.described() {
    get("/peer") {
        val peer = call.attributes[IpcHttpCallAttributes.peer]
        val address = call.attributes[IpcHttpCallAttributes.address]
        call.respondText(listOf(peer.user, peer.processId, address).joinToString(separator = " "))
    }
    get("/local") {
        val local = call.request.local
        val parts = listOf(
            local.scheme,
            local.version,
            local.method.value,
            local.uri,
            local.localHost,
            local.localPort,
            local.localAddress,
            local.serverHost,
            local.serverPort,
            local.remoteHost,
            local.remotePort,
            local.remoteAddress,
        )
        call.respondText(parts.joinToString(separator = "|"))
    }
    get("/headers") {
        call.response.header(name = "X-Konnect", value = "yes")
        val headers = call.response.headers.allValues()
        call.respondText(headers.names().joinToString())
    }
    get("/origin") { call.respondText(call.request.origin.remoteAddress) }
    get("/query") {
        val decoded = call.request.queryParameters.entries()
        val raw = call.request.rawQueryParameters.entries()
        call.respondText("$decoded $raw ${call.request.cookies["flavour"]}")
    }
}

private val CLIENT: String = checkNotNull(TestIpc.CLIENT.user).toString()

class CallTest : FunSpec({
    context("a call") {
        test("carries the peer the IPC server verified and the address it came in on") {
            served(routes = { described() }) { running ->
                running.connect().exchange(get("/peer")).body shouldBe "$CLIENT 2 $SERVICE"
            }
        }

        test("names the IPC address as local and the peer as remote") {
            served(routes = { described() }) { running ->
                val response = running.connect().exchange(get("/local?x=1", "Host: konnect.local:8080"))

                response.body shouldBe "http|HTTP/1.1|GET|/local?x=1|localhost|80|$SERVICE|konnect.local|8080|" +
                    "$CLIENT|0|process:2"
            }
        }

        test("falls back to defaults without a host header and with an unknown peer") {
            served(routes = { described() }) { running ->
                val response = running.connect(IpcPeer.UNKNOWN).exchange(get("/local"))

                response.body shouldBe "http|HTTP/1.1|GET|/local|localhost|80|$SERVICE|localhost|80|unknown|0|unknown"
            }
        }

        test("lists the headers of its answer") {
            served(routes = { described() }) { running ->
                running.connect().exchange(get("/headers")).body shouldBe "X-Konnect"
            }
        }

        test("is its own origin") {
            served(routes = { described() }) { running ->
                running.connect(TestIpc.SERVER).exchange(get("/origin")).body shouldBe "process:1"
            }
        }

        test("decodes its query and its cookies") {
            served(routes = { described() }) { running ->
                val response = running.connect().exchange(get("/query?name=a%20b&flag", "Cookie: flavour=mint"))

                response.body shouldBe "[name=[a b], flag=[]] [name=[a%20b], flag=[]] mint"
            }
        }

        test("with a query that cannot be decoded is refused with constant text") {
            served(routes = { described() }) { running ->
                val response = running.connect().exchange(get("/query?name=%zzsecret-of-the-peer%"))

                response.status shouldBe HttpStatusCode.BadRequest.value
                response.body shouldBe "The query of the request is malformed"
                response.body shouldNotContain "secret"
            }
        }
    }
})
