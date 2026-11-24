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

package com.davils.konnect.ipc.server.builder

import com.davils.konnect.ipc.core.access.IpcAccess
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.checkIpcAddress
import com.davils.konnect.ipc.core.checkIpcBufferSize
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.IpcServerViolationCodes
import com.davils.konnect.ipc.server.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val NO_ADDRESS: String = "none"

/**
 * How a server listens: its address, who may connect, how many connections it keeps and where it reports.
 *
 * A configuration is immutable and safe to share between threads. One that came out of `build()` was validated; one
 * read from `validate()` may still hold violations.
 *
 * @since 1.0.0
 */
public class IpcServerConfiguration internal constructor(builder: IpcServerBuilder) : Validatable {
    private val declaredAddress: IpcAddress? = builder.address

    /**
     * The address the server listens on.
     *
     * @throws IllegalStateException if no address was declared, which a validated configuration rules out.
     * @since 1.0.0
     */
    public val address: IpcAddress get() = checkNotNull(declaredAddress) { "The configuration was not validated" }

    /**
     * Who may connect; every peer is checked against it before its connection is handed out.
     *
     * @since 1.0.0
     */
    public val access: IpcAccess = builder.access

    /**
     * The most connections the server keeps open at the same time, from 1 to [Limits.MAX_CONNECTIONS]; an accept
     * waits while all of them are taken.
     *
     * @since 1.0.0
     */
    public val maxConnections: Int = builder.maxConnections

    /**
     * The size of the buffers of every connection, in bytes, from
     * [com.davils.konnect.ipc.core.Limits.MIN_BUFFER_SIZE] to [com.davils.konnect.ipc.core.Limits.MAX_BUFFER_SIZE].
     *
     * @since 1.0.0
     */
    public val bufferSize: Int = builder.bufferSize

    /**
     * Where the server reports: its meters, its events on [com.davils.konnect.ipc.server.IpcServerTopics.EVENTS] and
     * its spans.
     *
     * @since 1.0.0
     */
    public val telemetry: IpcTelemetry = builder.telemetry

    /**
     * Reports a missing address, a buffer size outside of its limits and a number of connections outside of 1 and
     * [Limits.MAX_CONNECTIONS], each with a message that names the limits and never the value it refused.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkIpcAddress(::address, declaredAddress)
        checkIpcBufferSize(::bufferSize, bufferSize)
        if (maxConnections in 1..Limits.MAX_CONNECTIONS) return
        fail(
            property = ::maxConnections,
            message = "must be from 1 to ${Limits.MAX_CONNECTIONS}",
            code = IpcServerViolationCodes.INVALID_MAX_CONNECTIONS,
        )
    }

    /**
     * Shows the address, the access and the bounds.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcServerConfiguration(address=${declaredAddress ?: NO_ADDRESS}, " +
        "access=$access, maxConnections=$maxConnections, bufferSize=$bufferSize, telemetry=$telemetry)"
}
