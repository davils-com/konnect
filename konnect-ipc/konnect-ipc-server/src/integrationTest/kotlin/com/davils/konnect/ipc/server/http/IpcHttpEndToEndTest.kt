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

package com.davils.konnect.ipc.server.http

import com.davils.konnect.ipc.client.http.IpcHttpUpgrade
import com.davils.konnect.ipc.client.http.IpcHttpUpgradeCapability
import com.davils.konnect.ipc.core.IpcPlatform
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.transport.windows.PipeSession
import com.davils.konnect.ipc.server.fixture.isUnix
import com.davils.konnect.ipc.server.fixture.isWindows
import com.davils.konnect.ipc.server.fixture.socketAddress
import com.davils.konnect.ipc.server.fixture.uniqueAddress
import com.davils.konnect.ipc.server.http.fixture.Greeting
import com.davils.konnect.ipc.server.http.fixture.sample
import com.davils.konnect.ipc.server.http.fixture.served
import com.davils.konnect.ipc.server.http.fixture.talking
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readLine
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.io.bytestring.encodeToByteString

private val streamed = object : OutgoingContent.WriteChannelContent() {
    override val contentType: ContentType = ContentType.Text.Plain

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeStringUtf8("streamed ")
        channel.flush()
        channel.writeStringUtf8("body")
    }
}

private suspend fun conversation(client: HttpClient) {
    client.get("http://konnect/ping").bodyAsText() shouldBe "pong"
    client.post("http://konnect/echo") { setBody("known") }.bodyAsText() shouldBe "known"
    client.post("http://konnect/echo") { setBody(streamed) }.bodyAsText() shouldBe "streamed body"
    val lines = client.get("http://konnect/lines")
    lines.headers[HttpHeaders.TransferEncoding] shouldBe "chunked"
    lines.bodyAsText() shouldBe "first\nsecond\n"
}

private suspend fun switched(client: HttpClient, heard: CompletableDeferred<String?>) {
    val welcomed = CompletableDeferred<Unit>()
    val input = flow {
        welcomed.await()
        emit("hello\n".encodeToByteString())
    }
    val statement = client.prepareGet("http://konnect/switch") {
        setCapability(IpcHttpUpgradeCapability, IpcHttpUpgrade(Greeting.PROTOCOL, input))
    }
    statement.execute { response ->
        response.status shouldBe HttpStatusCode.SwitchingProtocols
        response.bodyAsChannel().readLine() shouldBe "welcome"
        welcomed.complete(Unit)
        heard.await() shouldBe "hello"
    }
}

@OptIn(DavilsInternal::class)
private fun currentUser(): String {
    if (isWindows) {
        val session = PipeSession.open(IpcPlatform.namedPipes, IpcPlatform.currentProcessId).shouldNotBeNull()
        return session.user.toString()
    }
    return IpcPlatform.environment.userName
}

@OptIn(DavilsInternal::class)
private suspend fun spoken(address: IpcAddress) {
    val heard = CompletableDeferred<String?>()
    served(address, routes = { sample(heard) }) { threads ->
        talking(address, threads) { client ->
            conversation(client)
            client.get("http://konnect/peer").bodyAsText() shouldContain currentUser()
            switched(client, heard)
        }
    }
}

class IpcHttpEndToEndTest : FunSpec({
    context("the engine on a unix domain socket").config(enabledIf = { isUnix }) {
        test("talks to the IPC client engine with every framing, names the peer and switches protocols") {
            spoken(socketAddress())
        }
    }

    context("the engine on a windows named pipe").config(enabledIf = { isWindows }) {
        test("talks to the IPC client engine with every framing, names the peer and switches protocols") {
            spoken(uniqueAddress())
        }
    }
})
