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

package com.davils.konnect.ipc.core.telemetry

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.Builder

/**
 * The builder behind [ipcTelemetry], which collects where a client or server reports what it does.
 *
 * A builder is not thread-safe; it is meant to be configured and built by one thread. Once it was built, every
 * change throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IpcTelemetryBuilder internal constructor() : Builder<IpcTelemetry> {
    private var isBuilt: Boolean = false

    /**
     * See [IpcTelemetry.tracer].
     *
     * @throws IllegalStateException if this builder was built already.
     * @since 1.0.0
     */
    public var tracer: Tracer = Tracer.None
        set(value) {
            checkOpen()
            field = value
        }

    internal var meters: MeterRegistry? = null
        private set

    internal var events: EventBus? = null
        private set

    /**
     * Records the meters of a client or server in [registry]; see [IpcTelemetry.meters].
     *
     * @param registry the meter registry.
     * @throws IllegalStateException if this builder was built already.
     * @since 1.0.0
     */
    public fun meters(registry: MeterRegistry) {
        checkOpen()
        meters = registry
    }

    /**
     * Publishes the events of a client or server on [bus]; see [IpcTelemetry.events].
     *
     * @param bus the event bus.
     * @throws IllegalStateException if this builder was built already.
     * @since 1.0.0
     */
    public fun events(bus: EventBus) {
        checkOpen()
        events = bus
    }

    /**
     * Builds the telemetry; a second call builds an equal telemetry again.
     *
     * @return the telemetry.
     * @since 1.0.0
     */
    override fun build(): IpcTelemetry {
        isBuilt = true
        return IpcTelemetry(this)
    }

    internal fun from(base: IpcTelemetry) {
        tracer = base.tracer
        meters = base.meters
        events = base.events
    }

    private fun checkOpen() {
        check(!isBuilt) { "This builder was already built, so nothing in it can be changed any more" }
    }
}
