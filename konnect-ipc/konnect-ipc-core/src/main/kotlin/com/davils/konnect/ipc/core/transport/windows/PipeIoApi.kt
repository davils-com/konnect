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
 * The input and output of an open pipe handle.
 *
 * Every function returns the result of the operating system and never throws; a failure carries its Win32 error.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface PipeIoApi {
    /**
     * Reads at most [length] bytes into the start of the direct [buffer], blocking until at least one arrived.
     *
     * @param handle the pipe.
     * @param buffer a direct buffer of at least [length] bytes.
     * @param length the most bytes to read.
     * @return how many bytes were read, or the error.
     * @since 1.0.0
     */
    public fun read(handle: Long, buffer: ByteBuffer, length: Int): NativeResult

    /**
     * Writes the first [length] bytes of the direct [buffer]; `0` bytes write an empty message.
     *
     * @param handle the pipe.
     * @param buffer a direct buffer of at least [length] bytes.
     * @param length how many bytes to write.
     * @return how many bytes were written, or the error.
     * @since 1.0.0
     */
    public fun write(handle: Long, buffer: ByteBuffer, length: Int): NativeResult

    /**
     * Cancels every pending operation of [handle]; may be called from any thread.
     *
     * @param handle the pipe.
     * @return `0`, or the error.
     * @since 1.0.0
     */
    public fun cancel(handle: Long): NativeResult

    /**
     * Closes [handle].
     *
     * @param handle the pipe.
     * @return `0`, or the error.
     * @since 1.0.0
     */
    public fun close(handle: Long): NativeResult
}
