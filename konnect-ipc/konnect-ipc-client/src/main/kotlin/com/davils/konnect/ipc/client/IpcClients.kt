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

import com.davils.konnect.ipc.client.builder.IpcClientBuilder
import com.davils.konnect.ipc.client.transport.ConnectTransports
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Builds a client of a local server in a DSL, such as
 * `ipcClient(Dispatchers.IO) { address = IpcAddress.Named(IpcName("helper")) }`.
 *
 * The client connects over a Unix domain socket on Linux, macOS and FreeBSD and over a Windows named pipe on Windows;
 * on any other platform every attempt ends as [IpcConnectResult.Unavailable].
 *
 * @param dispatcher the dispatcher the blocking calls of the operating system run on, such as `Dispatchers.IO`; the
 *   client starts no threads of its own.
 * @param block declares the configuration.
 * @return the client.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is invalid; the report names
 *   every problem, such as a missing address, a buffer size outside of its limits or a connect timeout that is not
 *   positive and finite.
 * @since 1.0.0
 */
public fun ipcClient(dispatcher: CoroutineDispatcher, block: IpcClientBuilder.() -> Unit): IpcClient {
    val builder = IpcClientBuilder()
    builder.block()
    return IpcClient(builder.build(), ConnectTransports.forCurrentPlatform(dispatcher))
}
