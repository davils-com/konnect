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

package com.davils.konnect.ipc.server.http

import com.davils.konnect.ipc.server.outcome.IpcBindResult
import java.io.IOException

/**
 * Thrown when the engine starts and its IPC server cannot listen on its address.
 *
 * The message names [result], which holds constant text only.
 *
 * @property result why the server could not listen.
 * @since 1.0.0
 */
public class IpcHttpServerBindException(public val result: IpcBindResult) :
    IOException("The engine could not listen on its address: $result")
