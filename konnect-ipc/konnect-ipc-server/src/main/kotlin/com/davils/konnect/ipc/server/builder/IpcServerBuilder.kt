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
import com.davils.konnect.ipc.core.access.IpcAccessBuilder
import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.telemetry.IpcTelemetry
import com.davils.konnect.ipc.server.Limits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.konnect.ipc.core.Limits as CoreLimits

/**
 * The builder behind [com.davils.konnect.ipc.server.ipcServer], which collects where a server listens, who may
 * connect and how much it takes.
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one server. Once it was
 * evaluated by `build()` or `validate()`, nested blocks throw an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IpcServerBuilder internal constructor() : ValidatingBuilder<IpcServerConfiguration>() {
    override val subject: String get() = "ipc server"

    /**
     * See [IpcServerConfiguration.address]; it must be set.
     *
     * @since 1.0.0
     */
    public var address: IpcAddress? = null

    /**
     * See [IpcServerConfiguration.access]; [IpcAccess.CURRENT_USER] unless it is set.
     *
     * @since 1.0.0
     */
    public var access: IpcAccess = IpcAccess.CURRENT_USER

    /**
     * See [IpcServerConfiguration.maxConnections]; [Limits.DEFAULT_MAX_CONNECTIONS] unless it is set.
     *
     * @since 1.0.0
     */
    public var maxConnections: Int = Limits.DEFAULT_MAX_CONNECTIONS

    /**
     * See [IpcServerConfiguration.bufferSize]; [com.davils.konnect.ipc.core.Limits.DEFAULT_BUFFER_SIZE] unless it
     * is set.
     *
     * @since 1.0.0
     */
    public var bufferSize: Int = CoreLimits.DEFAULT_BUFFER_SIZE

    /**
     * See [IpcServerConfiguration.telemetry]; [IpcTelemetry.None] unless it is set.
     *
     * @since 1.0.0
     */
    public var telemetry: IpcTelemetry = IpcTelemetry.None

    /**
     * Builds the access in a nested block, such as `access { allow(IpcUser.UnixUser.ROOT) }`; its violations are
     * reported under `access`.
     *
     * @param block the block that says who may connect.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public fun access(block: IpcAccessBuilder.() -> Unit) {
        access = nested(IpcAccessBuilder(), ::access, block) ?: return
    }

    override fun assemble(): IpcServerConfiguration = IpcServerConfiguration(this)
}
