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

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.outcome
import java.io.IOException

/**
 * The engine [IpcHttp] could not open a connection for a request.
 *
 * The message names the kind of [result] in constant text, such as `not_found`, and never a reason or a peer.
 *
 * @property result why the connector made no connection.
 * @since 1.0.0
 */
public class IpcHttpConnectException(public val result: IpcConnectResult) :
    IOException("The engine could not connect to the server: ${result.outcome}")
