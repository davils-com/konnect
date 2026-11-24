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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.konnect.ipc.core.connection.IpcSystemError
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet

/**
 * The Win32 errors the pipe transports tell apart.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object Win32Error {
    /**
     * `ERROR_FILE_NOT_FOUND`: no pipe of that name exists.
     *
     * @since 1.0.0
     */
    public const val FILE_NOT_FOUND: Long = 2

    /**
     * `ERROR_ACCESS_DENIED`: the security descriptor refuses the caller, or the name is taken.
     *
     * @since 1.0.0
     */
    public const val ACCESS_DENIED: Long = 5

    /**
     * `ERROR_BROKEN_PIPE`: the other side has closed the pipe.
     *
     * @since 1.0.0
     */
    public const val BROKEN_PIPE: Long = 109

    /**
     * `ERROR_SEM_TIMEOUT`: no instance of the pipe became free in time.
     *
     * @since 1.0.0
     */
    public const val SEM_TIMEOUT: Long = 121

    /**
     * `ERROR_PIPE_BUSY`: every instance of the pipe is in use.
     *
     * @since 1.0.0
     */
    public const val PIPE_BUSY: Long = 231

    /**
     * `ERROR_NO_DATA`: the pipe is being closed.
     *
     * @since 1.0.0
     */
    public const val NO_DATA: Long = 232

    /**
     * `ERROR_PIPE_NOT_CONNECTED`: no process is on the other side.
     *
     * @since 1.0.0
     */
    public const val PIPE_NOT_CONNECTED: Long = 233

    /**
     * `ERROR_PIPE_CONNECTED`: a client connected before the server waited for it.
     *
     * @since 1.0.0
     */
    public const val PIPE_CONNECTED: Long = 535

    /**
     * `ERROR_OPERATION_ABORTED`: the operation was cancelled.
     *
     * @since 1.0.0
     */
    public const val OPERATION_ABORTED: Long = 995

    /**
     * The errors that mean the stream ended rather than failed.
     *
     * @since 1.0.0
     */
    public val endOfStream: Set<Long> =
        listOf(BROKEN_PIPE, NO_DATA, PIPE_NOT_CONNECTED, OPERATION_ABORTED).immutableSet()

    /**
     * Describes the Win32 error [code].
     *
     * @param code the error code.
     * @return the error, described as `Win32 error <code>`.
     * @since 1.0.0
     */
    public fun errorOf(code: Long): IpcSystemError = IpcSystemError(code = code, description = "Win32 error $code")
}
