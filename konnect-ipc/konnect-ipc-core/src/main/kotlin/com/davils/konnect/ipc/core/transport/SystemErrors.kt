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

package com.davils.konnect.ipc.core.transport

import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException

private const val FALLBACK_DESCRIPTION: String = "IOException"

/**
 * Describes this failure of the JDK as an [IpcSystemError] without a code.
 *
 * The description is the simple name of the class of the exception, such as `ClosedChannelException`, and never its
 * message, which may quote a path or a name that came from outside.
 *
 * @receiver the failure.
 * @return the error.
 * @since 1.0.0
 */
@DavilsInternal
public fun IOException.toSystemError(): IpcSystemError {
    val type = javaClass.simpleName
    val description = type.ifEmpty { FALLBACK_DESCRIPTION }
    return IpcSystemError(code = null, description = description)
}
