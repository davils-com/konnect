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
import com.davils.konnect.ipc.client.http.fixture.BASE
import com.davils.konnect.ipc.client.http.fixture.answer
import com.davils.konnect.ipc.client.http.fixture.wired
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.coroutines.CoroutineContext

private const val ATTACH: String = "$BASE/containers/web/attach"

private val switch = IpcHttpUpgrade("tcp", flowOf("std".encodeToByteString(), "in".encodeToByteString()))

private val foreign = object : OutgoingContent.ProtocolUpgrade() {
    override suspend fun upgrade(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        engineContext: CoroutineContext,
        userContext: CoroutineContext,
    ): Job = Job()
}

private suspend fun switchedOf(client: HttpClient, upgrade: IpcHttpUpgrade = switch): Pair<HttpStatusCode, String> {
    val statement = client.preparePost(ATTACH) { setCapability(IpcHttpUpgradeCapability, upgrade) }
    return statement.execute { response ->
        val bytes = response.bodyAsChannel().toByteArray()
        response.status to bytes.decodeToString()
    }
}

class IpcHttpUpgradeTest : FunSpec({
    context("an upgrade") {
        test("asks to switch, reads what follows the switch and writes the input, then ends its output") {
            wired { wire ->
                val response = async { wire.client().use { client -> switchedOf(client) } }
                val peer = wire.accept()
                val request = peer.request()
                peer.answer(answer("101 UPGRADED", "Connection: Upgrade", "Upgrade: tcp", body = "stdout"))

                request.headers["connection"] shouldBe "Upgrade"
                request.headers["upgrade"] shouldBe "tcp"
                peer.rest() shouldBe "stdin"
                response.await() shouldBe (HttpStatusCode.SwitchingProtocols to "stdout")
            }
        }

        test("that the server does not take keeps its connection") {
            wired { wire ->
                wire.client().use { client ->
                    val refused = async { switchedOf(client, IpcHttpUpgrade("tcp")) }
                    val peer = wire.accept()
                    peer.request()
                    peer.answer(answer("200 OK", "Content-Length: 2", body = "no"))

                    refused.await() shouldBe (HttpStatusCode.OK to "no")
                }
                wire.connects shouldBe 1
            }
        }

        test("of Ktor itself is refused, because the engine switches through its own capability") {
            wired { wire ->
                shouldThrow<IllegalArgumentException> {
                    wire.client().use { client -> client.post(ATTACH) { setBody(foreign) } }
                }
            }
        }
    }

    context("the protocol of an upgrade") {
        test("is an HTTP token with an optional version") {
            IpcHttpUpgrade("websocket").protocol shouldBe "websocket"
            IpcHttpUpgrade("h2c/2").protocol shouldBe "h2c/2"
            IpcHttpUpgrade("Web-Socket_1.0~!").protocol shouldBe "Web-Socket_1.0~!"
            val longest = "a".repeat(Limits.MAX_UPGRADE_PROTOCOL_LENGTH)

            IpcHttpUpgrade(longest).protocol shouldBe longest
        }

        test("is refused when it is empty, too long or holds a separator, without quoting it") {
            val tooLong = "a".repeat(Limits.MAX_UPGRADE_PROTOCOL_LENGTH + 1)
            val separated = listOf("tcp\r\nX-Evil: 1", "t cp", "tcp;", "a@b", "a{b", "a[b", "a:b", "é")
            (listOf("", tooLong) + separated).forEach { protocol ->
                val failure = shouldThrow<IllegalArgumentException> { IpcHttpUpgrade(protocol) }

                failure.message shouldBe "An upgrade must name an HTTP token of at most 64 characters"
            }
        }
    }

    context("an upgrade as a value") {
        test("names its protocol but not its input and equals an upgrade of the same protocol and input") {
            val input = emptyFlow<ByteString>()

            IpcHttpUpgrade("tcp").toString() shouldBe "IpcHttpUpgrade(tcp)"
            IpcHttpUpgrade("tcp", input) shouldBe IpcHttpUpgrade("tcp", input)
            IpcHttpUpgrade("tcp", input).hashCode() shouldBe IpcHttpUpgrade("tcp", input).hashCode()
            IpcHttpUpgrade("tcp", input) shouldNotBe IpcHttpUpgrade("udp", input)
            IpcHttpUpgrade("tcp", input) shouldNotBe IpcHttpUpgrade("tcp", flowOf())
            IpcHttpUpgrade("tcp").equals("tcp") shouldBe false
            IpcHttpUpgradeCapability.toString() shouldBe "IpcHttpUpgradeCapability"
        }
    }
})
