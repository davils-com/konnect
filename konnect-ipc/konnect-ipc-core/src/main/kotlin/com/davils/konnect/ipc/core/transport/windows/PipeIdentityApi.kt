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
import java.nio.ByteBuffer

/**
 * Who is on the other side of a pipe.
 *
 * Every function returns the result of the operating system and never throws; a failure carries its Win32 error.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface PipeIdentityApi {
    /**
     * Reads the process of the client of a server pipe.
     *
     * @param handle the server end of the pipe.
     * @return the process identifier, or the error.
     * @since 1.0.0
     */
    public fun clientProcessId(handle: Long): NativeResult

    /**
     * Reads the process of the server of a client pipe.
     *
     * @param handle the client end of the pipe.
     * @return the process identifier, or the error.
     * @since 1.0.0
     */
    public fun serverProcessId(handle: Long): NativeResult

    /**
     * Writes the security identifier of the user of [processId] into the direct [buffer] as UTF-16LE.
     *
     * @param processId the process.
     * @param buffer a direct buffer of [com.davils.konnect.ipc.core.Limits.MAX_SID_LENGTH] characters.
     * @return how many characters were written, or the error.
     * @since 1.0.0
     */
    public fun processUserSid(processId: Long, buffer: ByteBuffer): NativeResult
}
