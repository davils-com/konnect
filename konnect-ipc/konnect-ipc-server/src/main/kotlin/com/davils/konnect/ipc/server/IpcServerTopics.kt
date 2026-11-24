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

package com.davils.konnect.ipc.server

import com.davils.konfetti.core.TopicKey

/**
 * The topics the servers of Konnect publish on.
 *
 * @since 1.0.0
 */
public object IpcServerTopics {
    /**
     * The topic of the events of every server, `konnect.ipc.server.events`.
     *
     * A server publishes on it only when its telemetry names an event bus; it declares the topic on that bus if the
     * bus does not declare it yet, and it builds no event while the topic has no consumer.
     *
     * @since 1.0.0
     */
    public val EVENTS: TopicKey<IpcServerEvent> = TopicKey("konnect.ipc.server.events")
}
