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

@file:OptIn(InternalAPI::class, DavilsInternal::class)

package com.davils.konnect.ipc.client.http

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.http.connection.ConnectionPool
import com.davils.konnect.ipc.client.http.connection.HttpConnection
import com.davils.konnect.ipc.client.http.exchange.BodyCopy
import com.davils.konnect.ipc.client.http.exchange.ChunkedBody
import com.davils.konnect.ipc.client.http.exchange.Framing
import com.davils.konnect.ipc.client.http.exchange.RequestWriter
import com.davils.konnect.ipc.client.http.exchange.ResponseReader
import com.davils.konnect.ipc.client.http.exchange.TimedInput
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.SocketTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

private const val NAME: String = "konnect-ipc-http"

/**
 * The engine [IpcHttp] creates: it sends every request as HTTP/1.1 over a connection of its connector and reads the
 * response from it.
 *
 * The engine keeps the connections of answered requests for the next ones while the server keeps them alive, uses at
 * most [IpcHttpConfig.maxConnections] connections at the same time and keeps at most
 * [IpcHttpConfig.maxIdleConnections] idle ones, each for at most [IpcHttpConfig.idleTimeout]. It supports the
 * `HttpTimeout` plugin and [IpcHttpUpgradeCapability].
 *
 * Everything the server sends is untrusted: a head larger than [IpcHttpConfig.maxResponseHeadSize], a head or a chunk
 * that does not follow HTTP/1.1, an ambiguous or malformed length and a body that ends early fail the request with an
 * exception whose message is constant text and never quotes the response, and the connection is closed. A connection
 * whose server cannot be reached fails with an [IpcHttpConnectException]. The engine never logs and starts no threads
 * of its own: it runs on the dispatcher of its configuration.
 *
 * @property config the configuration of the engine.
 * @since 1.0.0
 */
public class IpcHttpEngine internal constructor(
    override val config: IpcHttpConfig,
    timeSource: TimeSource = TimeSource.Monotonic,
) : HttpClientEngineBase(NAME) {
    private val pool: ConnectionPool = ConnectionPool(
        maxIdle = config.maxIdleConnections,
        maxConnections = config.maxConnections,
        idleTimeout = config.idleTimeout,
        timeSource = timeSource,
    )

    private val reader: ResponseReader = ResponseReader(config.maxResponseHeadSize)

    private val chunked: ChunkedBody = ChunkedBody(config.maxResponseHeadSize)

    /**
     * The capabilities the engine supports: `HttpTimeout` and [IpcHttpUpgradeCapability].
     *
     * @since 1.0.0
     */
    override val supportedCapabilities: Set<HttpClientEngineCapability<*>> =
        listOf(HttpTimeoutCapability, IpcHttpUpgradeCapability).immutableSet()

    /**
     * Sends the request [data] and reads the head of its response; the body is read while the caller reads it.
     *
     * @param data the request.
     * @return the response.
     * @throws IpcHttpConnectException if the connector made no connection.
     * @throws io.ktor.client.network.sockets.ConnectTimeoutException if no connection was free and open within the
     *   connect timeout of the request.
     * @throws java.net.SocketTimeoutException if the server sent nothing for longer than the socket timeout of the
     *   request.
     * @throws java.io.IOException if the connection failed or the server sent no valid response; the message never
     *   quotes what the server sent.
     * @throws IllegalArgumentException if the request carries a protocol upgrade of Ktor or a header with a line break.
     * @since 1.0.0
     */
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val timeouts = data.getCapabilityOrNull(HttpTimeoutCapability)
        val connection = acquire(data, timeouts?.connectTimeoutMillis)
        var isHandedOver = false
        try {
            val response = exchange(
                data = data,
                connection = connection,
                socketTimeout = timeouts?.socketTimeoutMillis,
                context = callContext,
            )
            isHandedOver = true
            return response
        } finally {
            discardUnless(isHandedOver, connection)
        }
    }

    /**
     * Closes the engine and its idle connections; a connection in use is closed when its request ends.
     *
     * @since 1.0.0
     */
    override fun close() {
        super.close()
        pool.closeAll()
    }

    private fun discardUnless(isHandedOver: Boolean, connection: HttpConnection) {
        if (isHandedOver) return
        pool.release(connection, isReusable = false)
    }

    private suspend fun acquire(data: HttpRequestData, limit: Long?): HttpConnection {
        if (limit == null) return pool.lease(::connected)
        return withTimeoutOrNull(limit) { pool.lease(::connected) } ?: throw ConnectTimeoutException(data)
    }

    private suspend fun connected(): HttpConnection {
        val result = config.connector.connect()
        val connected = result as? IpcConnectResult.Connected ?: throw IpcHttpConnectException(result)
        return HttpConnection(connected.connection, this)
    }

    private suspend fun exchange(
        data: HttpRequestData,
        connection: HttpConnection,
        socketTimeout: Long?,
        context: CoroutineContext,
    ): HttpResponseData {
        val requestTime = GMTDate()
        val upgrade = data.getCapabilityOrNull(IpcHttpUpgradeCapability)
        RequestWriter(connection, this).write(data, upgrade)
        val input = TimedInput.of(connection.input, socketTimeout) { SocketTimeoutException(data) }
        val head = reader.head(input)
        val framing = Framing.of(data.method, head)
        val isReusable = Framing.isReusable(framing, head)
        val length = head.contentLength ?: 0L
        val body = when (framing) {
            Framing.Raw -> switched(connection, upgrade, context)

            Framing.Empty -> empty(connection, isReusable)

            Framing.Length -> decoded(connection, isReusable, context) { output ->
                BodyCopy.exactly(input = input, output = output, length = length)
            }

            Framing.Chunked -> decoded(connection, isReusable, context) { output -> chunked.copy(input, output) }

            Framing.UntilClose -> decoded(connection, isReusable, context) { output -> input.copyTo(output) }
        }
        return HttpResponseData(
            statusCode = head.status,
            requestTime = requestTime,
            headers = head.headers,
            version = head.version,
            body = body,
            callContext = context,
        )
    }

    private fun empty(connection: HttpConnection, isReusable: Boolean): ByteReadChannel {
        pool.release(connection, isReusable)
        return ByteReadChannel.Empty
    }

    private fun decoded(
        connection: HttpConnection,
        isReusable: Boolean,
        context: CoroutineContext,
        copy: suspend (ByteWriteChannel) -> Unit,
    ): ByteReadChannel {
        val body = ByteChannel()
        val copied = CompletableDeferred<Unit>()
        val copying = launch {
            copy(body)
            body.flushAndClose()
            copied.complete(Unit)
        }
        copying.invokeOnCompletion { cause ->
            val isComplete = copied.isCompleted
            if (!isComplete) body.cancel(cause)
            pool.release(connection, isReusable && isComplete)
        }
        context.job.invokeOnCompletion { copying.cancel() }
        return body
    }

    private fun switched(
        connection: HttpConnection,
        upgrade: IpcHttpUpgrade?,
        context: CoroutineContext,
    ): ByteReadChannel {
        val writing = launch { upgrade?.let { switch -> written(connection, switch) } }
        context.job.invokeOnCompletion {
            writing.cancel()
            pool.release(connection, isReusable = false)
        }
        return connection.input
    }

    private suspend fun written(connection: HttpConnection, upgrade: IpcHttpUpgrade) {
        upgrade.input.collect { chunk -> connection.write(chunk.toByteArray()) }
        connection.closeOutput()
    }
}
