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

import com.davils.konnect.ipc.server.IpcServer
import com.davils.konnect.ipc.server.Limits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import io.ktor.server.engine.BaseApplicationEngine
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val NO_SERVER: String = "none"

/**
 * The configuration of the HTTP engine, set in the block of `embeddedServer(IpcHttpServer, ...) { }`.
 *
 * The engine serves on the IPC server it is given, which names the address, checks every peer and reports the meters
 * of the engine through its telemetry; connectors are refused. Every bound is checked against [Limits] when the
 * engine is created, and the engine is not created while one is violated. A configuration is not thread-safe; it is
 * meant to be set by one thread before the engine is created.
 *
 * @since 1.0.0
 */
public class IpcHttpServerConfig :
    BaseApplicationEngine.Configuration(),
    Validatable {
    /**
     * The IPC server the engine serves on; it must be set. The engine binds it when it starts and closes it when it
     * stops.
     *
     * @since 1.0.0
     */
    public var server: IpcServer? = null

    /**
     * How long the engine waits for the head of the next request on an open connection before it closes the
     * connection, from 1 ms to [Limits.MAX_TIMEOUT_MILLIS]; [Limits.DEFAULT_IDLE_TIMEOUT_MILLIS] unless it is set.
     *
     * @since 1.0.0
     */
    public var idleTimeout: Duration = Limits.DEFAULT_IDLE_TIMEOUT_MILLIS.milliseconds

    /**
     * How long the whole body of a request may take to arrive, from the end of its head to its last byte, from 1 ms to
     * [Limits.MAX_TIMEOUT_MILLIS]; [Limits.DEFAULT_REQUEST_BODY_TIMEOUT_MILLIS] unless it is set.
     *
     * A body that takes longer fails the read of the application, is answered with 408 Request Timeout unless the
     * application answered already, and closes the connection. The time also runs while the application does not
     * read a body larger than the buffer of the engine.
     *
     * @since 1.0.0
     */
    public var requestBodyTimeout: Duration = Limits.DEFAULT_REQUEST_BODY_TIMEOUT_MILLIS.milliseconds

    /**
     * The largest head of a request, its request line and headers, in bytes, from 1 to [Limits.MAX_HEADER_SIZE];
     * [Limits.DEFAULT_MAX_HEADER_SIZE] unless it is set. A larger head is answered with 431 and closes the
     * connection.
     *
     * @since 1.0.0
     */
    public var maxHeaderSize: Int = Limits.DEFAULT_MAX_HEADER_SIZE

    /**
     * The largest body of a request, in bytes, from 0 to [Limits.MAX_REQUEST_BODY_SIZE];
     * [Limits.DEFAULT_MAX_REQUEST_BODY_SIZE] unless it is set. A larger declared body is answered with 413 before it
     * is read; a chunked body that grows larger fails the read of the application with 413.
     *
     * @since 1.0.0
     */
    public var maxRequestBodySize: Long = Limits.DEFAULT_MAX_REQUEST_BODY_SIZE

    /**
     * The most requests the engine serves on one connection, pipelined or one after the other, from 1 to
     * [Limits.MAX_REQUESTS_PER_CONNECTION]; [Limits.DEFAULT_MAX_REQUESTS_PER_CONNECTION] unless it is set. The answer
     * to the last of them carries `Connection: close`, and requests the peer sent beyond it are never read.
     *
     * @since 1.0.0
     */
    public var maxRequestsPerConnection: Int = Limits.DEFAULT_MAX_REQUESTS_PER_CONNECTION

    /**
     * Reports a missing server, every bound outside of [Limits] and any connector, each with a message that names the
     * limits and never the value it refused.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (server == null) {
            fail(::server, "must name the IPC server to serve on", IpcHttpServerViolationCodes.MISSING_SERVER)
        }
        checkTimeout(::idleTimeout, idleTimeout, IpcHttpServerViolationCodes.INVALID_IDLE_TIMEOUT)
        checkTimeout(::requestBodyTimeout, requestBodyTimeout, IpcHttpServerViolationCodes.INVALID_REQUEST_BODY_TIMEOUT)
        validateSizes()
        if (connectors.isEmpty()) return
        fail(
            property = ::connectors,
            message = "must be empty, because the IPC server names the address",
            code = IpcHttpServerViolationCodes.UNEXPECTED_CONNECTOR,
        )
    }

    /**
     * Shows the server and the bounds.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcHttpServerConfig(server=${server ?: NO_SERVER}, idleTimeout=$idleTimeout, " +
        "requestBodyTimeout=$requestBodyTimeout, maxHeaderSize=$maxHeaderSize, " +
        "maxRequestBodySize=$maxRequestBodySize, maxRequestsPerConnection=$maxRequestsPerConnection)"

    internal fun boundServer(): IpcServer = checkNotNull(server) { "The configuration was not validated" }

    private fun ValidationScope.validateSizes() {
        if (maxHeaderSize !in 1..Limits.MAX_HEADER_SIZE) {
            fail(
                property = ::maxHeaderSize,
                message = "must be from 1 to ${Limits.MAX_HEADER_SIZE} bytes",
                code = IpcHttpServerViolationCodes.INVALID_MAX_HEADER_SIZE,
            )
        }
        if (maxRequestBodySize !in 0..Limits.MAX_REQUEST_BODY_SIZE) {
            fail(
                property = ::maxRequestBodySize,
                message = "must be from 0 to ${Limits.MAX_REQUEST_BODY_SIZE} bytes",
                code = IpcHttpServerViolationCodes.INVALID_MAX_REQUEST_BODY_SIZE,
            )
        }
        if (maxRequestsPerConnection in 1..Limits.MAX_REQUESTS_PER_CONNECTION) return
        fail(
            property = ::maxRequestsPerConnection,
            message = "must be from 1 to ${Limits.MAX_REQUESTS_PER_CONNECTION}",
            code = IpcHttpServerViolationCodes.INVALID_MAX_REQUESTS_PER_CONNECTION,
        )
    }

    private fun ValidationScope.checkTimeout(property: KProperty<*>, timeout: Duration, code: ViolationCode) {
        val isBounded = timeout.isPositive() && timeout <= Limits.MAX_TIMEOUT_MILLIS.milliseconds
        if (isBounded) return
        fail(property = property, message = "must be from 1 ms to ${Limits.MAX_TIMEOUT_MILLIS} ms", code = code)
    }
}
