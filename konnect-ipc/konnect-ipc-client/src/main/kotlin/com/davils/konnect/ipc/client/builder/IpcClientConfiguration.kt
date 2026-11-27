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

package com.davils.konnect.ipc.client.builder

import com.davils.konnect.ipc.client.IpcClientViolationCodes
import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.checkIpcAddress
import com.davils.konnect.ipc.core.checkIpcBufferSize
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.kontinuity.retry.RetryConfig
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration
import com.davils.konnect.ipc.core.Limits as CoreLimits

private const val NO_ADDRESS: String = "none"

private const val NO_RETRY: String = "none"

/**
 * The configuration of an [com.davils.konnect.ipc.client.IpcClient], built by
 * [com.davils.konnect.ipc.client.ipcClient].
 *
 * A configuration is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class IpcClientConfiguration internal constructor(builder: IpcClientBuilder) : Validatable {
    private val declaredAddress: IpcAddress? = builder.address

    /**
     * Where the server listens.
     *
     * @throws IllegalStateException if the configuration was not validated, which only a configuration that never
     *   left its builder can be.
     * @since 1.0.0
     */
    public val address: IpcAddress get() = checkNotNull(declaredAddress) { "The configuration was not validated" }

    /**
     * The users the server may run as; a server whose user is not one of them, or cannot be read, is left before a
     * byte is sent.
     *
     * @since 1.0.0
     */
    public val trust: IpcAccess = builder.trust

    /**
     * How long an attempt waits for the server to accept a connection; positive and finite.
     *
     * @since 1.0.0
     */
    public val connectTimeout: Duration = builder.connectTimeout

    /**
     * The size of the buffers of every connection, in bytes, from [CoreLimits.MIN_BUFFER_SIZE] to
     * [CoreLimits.MAX_BUFFER_SIZE].
     *
     * @since 1.0.0
     */
    public val bufferSize: Int = builder.bufferSize

    /**
     * Where the client reports its attempts.
     *
     * @since 1.0.0
     */
    public val telemetry: IpcTelemetry = builder.telemetry

    /**
     * How a connect is retried while the server is not there yet, or `null` to try once; by default
     * [IpcClientBuilder.DEFAULT_CONNECT_RETRY], so a client that starts with its server waits for it to listen.
     *
     * Only a result that may pass is retried: [com.davils.konnect.ipc.client.IpcConnectResult.NotFound],
     * [com.davils.konnect.ipc.client.IpcConnectResult.TimedOut] and
     * [com.davils.konnect.ipc.client.IpcConnectResult.Unavailable]. Every attempt waits at most [connectTimeout], is
     * traced and counted like a single connect, and the retry reports its events and meters through [telemetry]. The
     * result of the last attempt is returned when the attempts are used up.
     *
     * @since 1.1.0
     */
    public val connectRetry: RetryConfig? = builder.connectRetry

    /**
     * Reports a missing address with the code [com.davils.kore.core.validation.violation.ViolationCode.REQUIRED], a
     * buffer size outside of its limits with the code
     * [com.davils.konnect.ipc.core.IpcViolationCodes.INVALID_BUFFER_SIZE] and a connect timeout that is not positive
     * and finite with the code [IpcClientViolationCodes.INVALID_CONNECT_TIMEOUT]. No message quotes the value it
     * refused.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkIpcAddress(::address, declaredAddress)
        checkIpcBufferSize(::bufferSize, bufferSize)
        if (connectTimeout.isPositive() && connectTimeout.isFinite()) return
        fail(
            property = ::connectTimeout,
            message = "must be positive and finite",
            code = IpcClientViolationCodes.INVALID_CONNECT_TIMEOUT,
        )
    }

    /**
     * Shows the address, the trust, the timeout, the buffer size and the telemetry.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcClientConfiguration(address=${declaredAddress ?: NO_ADDRESS}, " +
        "trust=$trust, connectTimeout=$connectTimeout, bufferSize=$bufferSize, telemetry=$telemetry, connectRetry=${connectRetry ?: NO_RETRY})"
}
