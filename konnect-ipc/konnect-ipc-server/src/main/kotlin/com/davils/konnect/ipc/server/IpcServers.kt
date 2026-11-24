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

import com.davils.konnect.ipc.server.builder.IpcServerBuilder
import com.davils.konnect.ipc.server.transport.ListenTransports
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates a server of local connections in a DSL, such as `ipcServer(Dispatchers.IO) { address = myAddress }`.
 *
 * The server serves a Unix domain socket on Linux, macOS and FreeBSD and a named pipe on Windows; on any other
 * platform, or on Windows without the native library of named pipes, it binds to nothing and answers
 * [com.davils.konnect.ipc.server.outcome.IpcBindResult.Unavailable]. The server is created unbound; nothing touches
 * the operating system before [IpcServer.bind].
 *
 * @param dispatcher the dispatcher the blocking calls of the operating system run on, such as `Dispatchers.IO`; an
 *   accept holds one of its threads while it waits.
 * @param block the block that configures the server.
 * @return the server.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is invalid; the report
 *   holds every violation, with the codes of [IpcServerViolationCodes] and of
 *   [com.davils.konnect.ipc.core.IpcViolationCodes].
 * @since 1.0.0
 */
public fun ipcServer(dispatcher: CoroutineDispatcher, block: IpcServerBuilder.() -> Unit): IpcServer {
    val builder = IpcServerBuilder()
    builder.block()
    return IpcServer(builder.build(), ListenTransports.forCurrentPlatform(dispatcher))
}
