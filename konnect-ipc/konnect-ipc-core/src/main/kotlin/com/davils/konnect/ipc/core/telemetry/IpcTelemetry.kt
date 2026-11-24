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

/**
 * Where the clients and servers of Konnect report what they do: the meter registry, the event bus and the tracer.
 *
 * Konnect never logs; everything it has to say goes through these three. Telemetry holds no state of its own, so one
 * instance can serve every client and server of an application. Without a meter registry nothing is counted, without
 * an event bus nothing is published, and the [Tracer.None] tracer traces nothing. Instances are immutable and safe to
 * share between threads.
 *
 * @since 1.0.0
 */
public class IpcTelemetry internal constructor(builder: IpcTelemetryBuilder) {
    /**
     * The meter registry the meters of a client or server are recorded in, or `null` for none.
     *
     * @since 1.0.0
     */
    public val meters: MeterRegistry? = builder.meters

    /**
     * The event bus the events of a client or server are published on, or `null` for none.
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.events

    /**
     * The tracer that opens a span for every connection that is made or accepted; [Tracer.None] by default.
     *
     * @since 1.0.0
     */
    public val tracer: Tracer = builder.tracer

    /**
     * Shows which parts of the telemetry are set.
     *
     * @return the telemetry as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "IpcTelemetry(meters=${meters != null}, events=${events != null}, traced=${tracer != Tracer.None})"

    /**
     * The telemetry that reports nothing.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The telemetry without a meter registry, without an event bus and with the [Tracer.None] tracer.
         *
         * @since 1.0.0
         */
        public val None: IpcTelemetry = IpcTelemetry(IpcTelemetryBuilder())
    }
}
