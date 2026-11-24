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

package com.davils.konnect.ipc.server.http.fixture

import com.davils.konnect.ipc.server.http.IpcHttpCallAttributes
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.CompletableDeferred

internal fun Routing.sample(heard: CompletableDeferred<String?>) {
    get("/ping") { call.respondText("pong") }
    post("/echo") { call.respondText(call.receiveText()) }
    get("/lines") {
        call.respondTextWriter {
            write("first\n")
            flush()
            write("second\n")
        }
    }
    get("/peer") {
        val peer = call.attributes[IpcHttpCallAttributes.peer]
        call.respondText(peer.user?.toString().orEmpty())
    }
    get("/switch") { call.respond(Greeting(heard)) }
}
