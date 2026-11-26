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

package com.davils.konnect.ipc.client

import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.Tracer
import com.davils.konnect.ipc.client.builder.IpcClientConfiguration
import com.davils.konnect.ipc.client.transport.ConnectOutcome
import com.davils.konnect.ipc.client.transport.ConnectTransport
import com.davils.konnect.ipc.core.connection.IpcConnection
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.core.telemetry.traced
import com.davils.konnect.ipc.core.transport.IpcChannel
import com.davils.kontinuity.core.Environment
import com.davils.kontinuity.core.environment
import com.davils.kontinuity.retry.Retry
import com.davils.kontinuity.retry.RetryConfig
import com.davils.kontinuity.retry.retry
import com.davils.kontinuity.retry.retryConfig
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kontinuity.core.filter.results as resultFilter

private const val CONNECT_OPERATION: String = "ipc.connect"
private const val ADDRESS_ATTRIBUTE: String = "ipc.address"
private const val OUTCOME_ATTRIBUTE: String = "ipc.outcome"
private const val RETRY_NAME: String = "konnect.ipc.connect"

/**
 * A client that connects to the local server at the address of its [configuration] and checks the user of the server
 * against its trust before it hands out the connection.
 *
 * Every [connect] opens a new connection. The client opens a span `ipc.connect` of the kind
 * [SpanKind.Client] for every attempt, with the attributes `ipc.address` and `ipc.outcome`; it counts every attempt in
 * the counter `konnect.ipc.client.connects` (`konnect_ipc_client_connects_total` in Prometheus), labelled by `outcome`
 * such as `connected`, `not_found` or `server_rejected`, when its telemetry names a meter registry; and it
 * publishes an [IpcClientEvent.ServerRejected] when it leaves a server it does not trust and its telemetry names an
 * event bus. Telemetry never makes an attempt wait or fail, and nothing is logged. A client holds no connection
 * itself and is safe to use from several coroutines.
 *
 * With a [IpcClientConfiguration.connectRetry], a connect that finds no server, is not accepted in time or finds the
 * server unavailable is tried again by a retry of kontinuity named `konnect.ipc.connect`, which waits by its backoff
 * between the attempts and reports its events and meters through the telemetry of the configuration.
 *
 * @property configuration the validated configuration of the client.
 * @since 1.0.0
 */
public class IpcClient internal constructor(
    public val configuration: IpcClientConfiguration,
    private val transport: ConnectTransport,
) : IpcConnector {
    private val tracer: Tracer = configuration.telemetry.tracer

    private val address: String = configuration.address.toString()

    private val telemetry: ClientTelemetry = ClientTelemetry(configuration.telemetry, configuration.address)

    private val retry: Retry? = configuration.connectRetry?.let { base -> retryOf(base, configuration.telemetry) }

    /**
     * Connects to the server, waiting at most the connect timeout of the configuration.
     *
     * The user of the server is read from the operating system and checked against the trust of the configuration
     * before the connection is handed out; a server whose user is not trusted or cannot be read is left at once. With
     * a [IpcClientConfiguration.connectRetry], a transient result is tried again; the result of the last attempt is
     * returned.
     *
     * @return [IpcConnectResult.Connected] with a connection the caller owns, or why none was made.
     * @since 1.0.0
     */
    override suspend fun connect(): IpcConnectResult {
        val active = retry ?: return connectOnce()
        return active.execute { connectOnce() }
    }

    /**
     * Shows the address of the client.
     *
     * @return the client as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcClient($address)"

    private suspend fun connectOnce(): IpcConnectResult {
        val span = tracer.start(CONNECT_OPERATION, SpanKind.Client)
        span.attribute(key = ADDRESS_ATTRIBUTE, value = address)
        val result = span.traced {
            val attempt = attempted()
            span.attribute(key = OUTCOME_ATTRIBUTE, value = attempt.outcome)
            attempt
        }
        telemetry.connected(result)
        return result
    }

    private suspend fun attempted(): IpcConnectResult {
        val outcome = transport.connect(
            address = configuration.address,
            timeout = configuration.connectTimeout,
            bufferSize = configuration.bufferSize,
        )
        return when (outcome) {
            is ConnectOutcome.Refused -> outcome.result
            is ConnectOutcome.Connected -> trusted(outcome.channel)
        }
    }

    private fun trusted(channel: IpcChannel): IpcConnectResult {
        val peer = channel.peer
        if (configuration.trust.permits(user = peer.user, currentUser = transport.currentUser)) {
            return IpcConnectResult.Connected(IpcConnection(channel, configuration.bufferSize))
        }
        channel.close()
        return IpcConnectResult.ServerRejected(peer)
    }

    private companion object {
        fun retryOf(base: RetryConfig, telemetry: IpcTelemetry): Retry {
            val config = retryConfig(base) {
                results = resultFilter { failIf<IpcConnectResult> { result -> result.isTransient } }
            }
            return retry(name = RETRY_NAME, config = config, environment = resilienceOf(telemetry))
        }

        fun resilienceOf(telemetry: IpcTelemetry): Environment = environment {
            tracer = telemetry.tracer
            telemetry.events?.let { bus -> events(bus) }
            telemetry.meters?.let { registry -> meters(registry) }
        }
    }
}
