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

import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.http.exchange.request.RequestHead
import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.Headers
import io.ktor.http.HttpMethod

private const val DEFAULT_PORT: Int = 80

private fun pointOf(host: String): IpcConnectionPoint {
    val head = RequestHead(
        method = HttpMethod.Get,
        uri = "/",
        version = "HTTP/1.1",
        headers = Headers.build { append(name = "Host", value = host) },
    )
    return IpcConnectionPoint(head, CallOrigin(SERVICE, TestIpc.CLIENT))
}

class IpcConnectionPointTest : FunSpec({
    context("the host of a request") {
        test("without a port keeps the default port") {
            val point = pointOf("konnect")

            point.serverHost shouldBe "konnect"
            point.serverPort shouldBe DEFAULT_PORT
        }

        test("with a port that is no number keeps the default port") {
            pointOf("konnect:http").serverPort shouldBe DEFAULT_PORT
        }
    }

    context("the deprecated members") {
        @Suppress("DEPRECATION_ERROR")
        test("answer with the local host and port") {
            val point = pointOf("konnect:9000")

            point.host shouldBe "localhost"
            point.port shouldBe DEFAULT_PORT
            point.toString() shouldContain "remoteAddress=process:2"
        }
    }
})
