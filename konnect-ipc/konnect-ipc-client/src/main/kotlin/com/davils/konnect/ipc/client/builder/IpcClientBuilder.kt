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

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.access.IpcAccessBuilder
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.kontinuity.retry.RetryConfig
import com.davils.kontinuity.retry.RetryConfigBuilder
import com.davils.kontinuity.retry.retryConfig
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.davils.konnect.ipc.core.Limits as CoreLimits

/**
 * The builder behind [com.davils.konnect.ipc.client.ipcClient], which collects the configuration of a client.
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one client. Once it was
 * evaluated by `build()` or `validate()`, every change throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IpcClientBuilder internal constructor() : ValidatingBuilder<IpcClientConfiguration>() {
    override val subject: String get() = "ipc client"

    /**
     * See [IpcClientConfiguration.address]; it must be set.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public var address: IpcAddress? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IpcClientConfiguration.trust]; [IpcAccess.CURRENT_USER] unless it is set.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public var trust: IpcAccess = IpcAccess.CURRENT_USER
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IpcClientConfiguration.connectTimeout]; [DEFAULT_CONNECT_TIMEOUT] unless it is set.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public var connectTimeout: Duration = DEFAULT_CONNECT_TIMEOUT
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IpcClientConfiguration.bufferSize]; [CoreLimits.DEFAULT_BUFFER_SIZE] unless it is set.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public var bufferSize: Int = CoreLimits.DEFAULT_BUFFER_SIZE
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IpcClientConfiguration.telemetry]; [IpcTelemetry.None] unless it is set.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public var telemetry: IpcTelemetry = IpcTelemetry.None
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IpcClientConfiguration.connectRetry]; none unless it is set, so a client tries once.
     *
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.1.0
     */
    public var connectRetry: RetryConfig? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Retries a connect while the server is not there yet, such as `connectRetry { maxAttempts = 5 }`, with the retry
     * of kontinuity.
     *
     * The block configures the attempts, the backoff and its jitter, a retry budget and how failures of the transport
     * count. Which results are worth another attempt is decided by the client and replaces the results of the block: a
     * server that is not found, does not accept in time or is unavailable is tried again, a server that is not trusted,
     * that refuses access or a failure of the system never is.
     *
     * @param block configures the retry, starting from the defaults of kontinuity.
     * @throws IllegalStateException if this builder was evaluated already.
     * @throws com.davils.kore.core.validation.report.ValidationException if the block configures an invalid retry.
     * @since 1.1.0
     */
    public fun connectRetry(block: RetryConfigBuilder.() -> Unit) {
        connectRetry = retryConfig(configure = block)
    }

    /**
     * Declares the users the server may run as in a nested block, such as `trust { allow(IpcUser.UnixUser.ROOT) }`.
     *
     * The current user is trusted unless the block sets [IpcAccessBuilder.isCurrentUserAllowed] to `false`. An access
     * that trusts nobody is reported at `trust`, and the trust stays as it was.
     *
     * @param block the block that says whom to trust.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public fun trust(block: IpcAccessBuilder.() -> Unit) {
        trust = nested(IpcAccessBuilder(), ::trust, block) ?: return
    }

    override fun assemble(): IpcClientConfiguration = IpcClientConfiguration(this)

    /**
     * The defaults of a client.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * How long a client waits for a server to accept a connection unless another time is given: 5 seconds.
         *
         * @since 1.0.0
         */
        public val DEFAULT_CONNECT_TIMEOUT: Duration = 5.seconds
    }
}
