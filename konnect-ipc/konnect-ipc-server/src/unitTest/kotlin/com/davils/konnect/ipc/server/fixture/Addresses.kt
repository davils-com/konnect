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

package com.davils.konnect.ipc.server.fixture

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcName
import java.nio.file.Path

internal val HELPER: IpcAddress = IpcAddress.Named(IpcName("helper"))

internal val SERVICE: IpcAddress = IpcAddress.Named(IpcName("konnect-service"))

internal val ROOT: Path = Path.of("").toAbsolutePath().root

internal val HELPER_DIRECTORY: Path = ROOT.resolve("tmp").resolve("konnect-alice")

internal val HELPER_SOCKET: Path = HELPER_DIRECTORY.resolve("helper.sock")

internal val SOCKET: IpcAddress = IpcAddress.UnixSocket(ROOT.resolve("run").resolve("konnect.sock"))
