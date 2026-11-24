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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.platform.NativeResult

/**
 * The named pipes of Windows.
 *
 * Every function returns the result of the operating system and never throws; a failure carries its Win32 error.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface NamedPipeApi :
    PipeIoApi,
    PipeIdentityApi {
    /**
     * Creates an instance of the server pipe at [path], which rejects remote clients.
     *
     * @param path the full path of the pipe, such as `\\.\pipe\name`.
     * @param securityDescriptor the security descriptor of the pipe in SDDL.
     * @param bufferSize the size of the input and output buffers, in bytes.
     * @param isFirstInstance whether the instance must be the first of its name, so no other process holds it.
     * @return the handle of the instance, or the error.
     * @since 1.0.0
     */
    public fun createServer(
        path: String,
        securityDescriptor: String,
        bufferSize: Int,
        isFirstInstance: Boolean,
    ): NativeResult

    /**
     * Waits until a client connects to the server pipe [handle], blocking until it did or the wait was cancelled.
     *
     * @param handle the server pipe.
     * @return `0`, or the error.
     * @since 1.0.0
     */
    public fun awaitClient(handle: Long): NativeResult

    /**
     * Opens the client end of the pipe at [path], for identification only.
     *
     * @param path the full path of the pipe.
     * @return the handle, or the error.
     * @since 1.0.0
     */
    public fun open(path: String): NativeResult

    /**
     * Waits up to [timeoutMillis] milliseconds until an instance of the pipe at [path] is free.
     *
     * @param path the full path of the pipe.
     * @param timeoutMillis the longest wait, in milliseconds; not negative.
     * @return `0`, or the error.
     * @since 1.0.0
     */
    public fun waitAvailable(path: String, timeoutMillis: Int): NativeResult
}
