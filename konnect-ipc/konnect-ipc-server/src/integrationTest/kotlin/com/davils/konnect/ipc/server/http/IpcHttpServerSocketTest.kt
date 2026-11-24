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

package com.davils.konnect.ipc.server.http

import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.server.fixture.isUnix
import com.davils.konnect.ipc.server.fixture.socketAddress
import com.davils.konnect.ipc.server.http.fixture.Greeting
import com.davils.konnect.ipc.server.http.fixture.RawSocket
import com.davils.konnect.ipc.server.http.fixture.http
import com.davils.konnect.ipc.server.http.fixture.sample
import com.davils.konnect.ipc.server.http.fixture.served
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.CompletableDeferred

private const val CLOSE: String = "Connection: close"
private const val CHUNKED: String = "Transfer-Encoding: chunked"
private val HEAD_END: String = http("", "", "")

class IpcHttpServerSocketTest : FunSpec({
    context("the engine on a unix domain socket").config(enabledIf = { isUnix }) {
        test("answers HTTP/1.1 a raw socket writes with every framing and names the peer") {
            val address = socketAddress()
            served(address, routes = { sample(CompletableDeferred()) }) {
                RawSocket(address.path).use { socket ->
                    socket.send(http("GET /ping HTTP/1.1", "Host: konnect", "", ""))
                    socket.send(http("POST /echo HTTP/1.1", "Content-Length: 5", "", "known"))
                    socket.send(
                        http("POST /echo HTTP/1.1", CHUNKED, "", "4", "stre", "4", "amed", "0", "", ""),
                    )
                    socket.send(http("GET /lines HTTP/1.1", "", ""))
                    socket.send(http("GET /peer HTTP/1.1", CLOSE, "", ""))

                    val answers = socket.rest()

                    answers shouldStartWith http("HTTP/1.1 200 OK", "")
                    answers shouldContain "${HEAD_END}pong"
                    answers shouldContain "${HEAD_END}known"
                    answers shouldContain "${HEAD_END}streamed"
                    answers shouldContain CHUNKED
                    answers shouldContain "first\n"
                    answers shouldContain CLOSE
                    answers shouldEndWith "$HEAD_END${IpcPlatform.environment.userName}"
                }
            }
        }

        test("refuses a malformed request with constant text and closes the connection") {
            val address = socketAddress()
            served(address, routes = { sample(CompletableDeferred()) }) {
                RawSocket(address.path).use { socket ->
                    socket.send(http("NOT A REQUEST\u0001secret", "", ""))

                    socket.rest() shouldBe http(
                        "HTTP/1.1 400 Bad Request",
                        "Content-Length: 0",
                        CLOSE,
                        "",
                        "",
                    )
                }
            }
        }

        test("switches the protocol of a raw socket") {
            val address = socketAddress()
            val heard = CompletableDeferred<String?>()
            served(address, routes = { sample(heard) }) {
                RawSocket(address.path).use { socket ->
                    socket.send(
                        http("GET /switch HTTP/1.1", "Connection: Upgrade", "Upgrade: ${Greeting.PROTOCOL}", "", ""),
                    )

                    val head = socket.readUntil(HEAD_END)
                    head shouldStartWith "HTTP/1.1 101 Switching Protocols"
                    socket.readUntil("\n") shouldBe "welcome\n"
                    socket.send("hello\n")
                    heard.await() shouldBe "hello"
                }
            }
        }
    }
})
