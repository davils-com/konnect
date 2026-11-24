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

package com.davils.konnect.ipc.client.http.fixture

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.IpcConnector
import com.davils.konnect.ipc.client.http.IpcHttp
import com.davils.konnect.ipc.client.http.IpcHttpConfig
import com.davils.konnect.ipc.client.http.IpcHttpEngine
import com.davils.konnect.testing.ipc.TestIpc
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal const val BASE: String = "http://docker/v1.56"

internal const val PING: String = "$BASE/_ping"

@OptIn(ExperimentalAtomicApi::class)
internal class Wire(scope: CoroutineScope) {
    private val accepted: Channel<Peer> = Channel(Channel.UNLIMITED)

    private val connectCount = AtomicInt(0)

    val connects: Int get() = connectCount.load()

    val connector: IpcConnector = IpcConnector {
        connectCount.addAndFetch(1)
        val pair = TestIpc.connections()
        accepted.send(Peer(pair.server, scope))
        IpcConnectResult.Connected(pair.client)
    }

    fun client(maxIdle: Int = 2, block: IpcHttpConfig.() -> Unit = {}): HttpClient = HttpClient(IpcHttp) {
        followRedirects = false
        engine {
            connector = this@Wire.connector
            maxIdleConnections = maxIdle
            block()
        }
    }

    fun clientOn(timeSource: TimeSource, block: IpcHttpConfig.() -> Unit = {}): HttpClient {
        val config = IpcHttpConfig()
        config.connector = connector
        config.block()
        return HttpClient(IpcHttpEngine(config, timeSource)) { followRedirects = false }
    }

    suspend fun accept(): Peer = withTimeout(5.seconds) { accepted.receive() }
}
