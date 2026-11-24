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

/**
 * Where a server is in its life.
 *
 * A server starts [Created], moves to [Listening] once it was bound and to [Closed] once it was closed; it never goes
 * back.
 *
 * @since 1.0.0
 */
public enum class IpcServerState {
    /**
     * The server was created and does not listen yet.
     *
     * @since 1.0.0
     */
    Created,

    /**
     * The server listens on its address and accepts connections.
     *
     * @since 1.0.0
     */
    Listening,

    /**
     * The server was closed and neither listens nor binds again.
     *
     * @since 1.0.0
     */
    Closed,
}
