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

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.IpcConnector
import com.davils.konnect.ipc.client.Limits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.inRange
import com.davils.kore.core.validation.positive
import io.ktor.client.engine.HttpClientEngineConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val NO_CONNECTOR: String = "the engine names no connector"

/**
 * The configuration of the engine [IpcHttp].
 *
 * Every bound is checked when the engine is created. The timeouts of a request come from the `HttpTimeout` plugin of
 * Ktor: the connect timeout covers the wait for a free connection and the connect, and the socket timeout covers every
 * read of the head and the body of a response that waits for the server. A configuration is meant to be set by one
 * thread, inside the `engine { }` block of an `HttpClient`.
 *
 * @since 1.0.0
 */
public class IpcHttpConfig : HttpClientEngineConfig(), Validatable {
    /**
     * What opens the connections, such as an [com.davils.konnect.ipc.client.IpcClient]; one that answers
     * [IpcConnectResult.Unavailable] unless it is set.
     *
     * @since 1.0.0
     */
    public var connector: IpcConnector = IpcConnector { IpcConnectResult.Unavailable(NO_CONNECTOR) }

    /**
     * The most connections requests use at the same time, from 1 to [Limits.MAX_CONNECTIONS];
     * [Limits.DEFAULT_MAX_CONNECTIONS] unless it is set.
     *
     * A request that finds every connection in use waits for one, as long as its connect timeout allows and until it
     * is cancelled. The idle connections the engine keeps come on top, so the engine never holds more than this
     * number and [maxIdleConnections] together.
     *
     * @since 1.0.0
     */
    public var maxConnections: Int = Limits.DEFAULT_MAX_CONNECTIONS

    /**
     * The most idle connections the engine keeps for the next request, from 0 to [Limits.MAX_IDLE_CONNECTIONS];
     * [Limits.DEFAULT_MAX_IDLE_CONNECTIONS] unless it is set.
     *
     * @since 1.0.0
     */
    public var maxIdleConnections: Int = Limits.DEFAULT_MAX_IDLE_CONNECTIONS

    /**
     * How long an idle connection may wait for its next request, positive and finite;
     * [Limits.DEFAULT_IDLE_TIMEOUT_MILLIS] unless it is set.
     *
     * An idle connection that waited longer is closed when a request looks for one; the engine runs no timer of its
     * own.
     *
     * @since 1.0.0
     */
    public var idleTimeout: Duration = Limits.DEFAULT_IDLE_TIMEOUT_MILLIS.milliseconds

    /**
     * The largest head of a response, its status line and its headers, in bytes, from [Limits.MIN_RESPONSE_HEAD_SIZE]
     * to [Limits.MAX_RESPONSE_HEAD_SIZE]; [Limits.DEFAULT_MAX_RESPONSE_HEAD_SIZE] unless it is set.
     *
     * A longer head is refused with an exception before it is parsed, and the connection is closed. The same limit
     * applies to the trailers of a chunked body.
     *
     * @since 1.0.0
     */
    public var maxResponseHeadSize: Int = Limits.DEFAULT_MAX_RESPONSE_HEAD_SIZE

    /**
     * Reports every bound outside of its limits with the code
     * [com.davils.kore.core.validation.violation.ViolationCode.OUT_OF_RANGE]; the messages name the limits and never
     * the value they refused.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        inRange(::maxConnections, maxConnections, 1..Limits.MAX_CONNECTIONS)
        inRange(::maxIdleConnections, maxIdleConnections, 0..Limits.MAX_IDLE_CONNECTIONS)
        positive(::idleTimeout, idleTimeout)
        inRange(
            property = ::maxResponseHeadSize,
            value = maxResponseHeadSize,
            range = Limits.MIN_RESPONSE_HEAD_SIZE..Limits.MAX_RESPONSE_HEAD_SIZE,
        )
    }
}
