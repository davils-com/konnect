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

package com.davils.konnect.ipc.client.http

import io.ktor.client.engine.HttpClientEngineCapability

/**
 * The capability of a request that switches its connection to another protocol, such as the attach of the Docker
 * Engine, set with `setCapability(IpcHttpUpgradeCapability, IpcHttpUpgrade("tcp"))`.
 *
 * The request asks the server with `Connection: Upgrade` and `Upgrade: <protocol>`. When the server answers with
 * `101 Switching Protocols`, the body of the response carries every byte the server sends from then on, the input of
 * the upgrade is written to the server and the output is closed after it; the connection is never used again and is
 * closed with the call.
 *
 * @since 1.0.0
 */
public data object IpcHttpUpgradeCapability : HttpClientEngineCapability<IpcHttpUpgrade>
