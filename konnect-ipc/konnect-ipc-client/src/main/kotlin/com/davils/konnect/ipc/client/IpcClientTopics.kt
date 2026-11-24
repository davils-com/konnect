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

package com.davils.konnect.ipc.client

import com.davils.konfetti.core.TopicKey

/**
 * The topics a client publishes on.
 *
 * @since 1.0.0
 */
public object IpcClientTopics {
    /**
     * The topic of [IpcClientEvent], named `konnect.ipc.client`.
     *
     * A client declares it with the defaults of the bus when the bus does not declare it yet; an application that
     * wants other settings declares it before the first client publishes.
     *
     * @since 1.0.0
     */
    public val EVENTS: TopicKey<IpcClientEvent> = TopicKey("konnect.ipc.client")
}
