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

@file:OptIn(DavilsInternal::class)

package com.davils.konnect.ipc.client.fixture

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.konnect.ipc.core.address.IpcName
import com.davils.konnect.testing.ipc.FakeIpcEnvironment
import com.davils.kore.core.annotation.DavilsInternal
import java.nio.file.Path

private val root: Path = Path.of("").toAbsolutePath().root

private val helperDirectory: Path = FakeIpcEnvironment().temporaryDirectory.resolve("konnect-alice")

internal val HELPER: IpcAddress = IpcAddress.Named(IpcName("helper"))

internal val HELPER_SOCKET: Path = helperDirectory.resolve("helper.sock")

internal val SOCKET: IpcAddress = IpcAddress.UnixSocket(root.resolve("run").resolve("konnect.sock"))
