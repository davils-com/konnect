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

/**
 * Something that opens a connection to a local server, such as an [IpcClient] or, in tests, a scripted connector.
 *
 * @since 1.0.0
 */
public fun interface IpcConnector {
    /**
     * Opens a connection to the server.
     *
     * @return the connection, or why none was made; a connector reports what fails as a result and does not throw.
     * @since 1.0.0
     */
    public suspend fun connect(): IpcConnectResult
}
