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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import java.nio.ByteBuffer

@DavilsPlatformBinding
internal object NamedPipeNative {
    @JvmStatic
    external fun createServer(path: String, securityDescriptor: String, bufferSize: Int, isFirstInstance: Boolean): Long

    @JvmStatic
    external fun awaitClient(handle: Long): Long

    @JvmStatic
    external fun open(path: String): Long

    @JvmStatic
    external fun waitAvailable(path: String, timeoutMillis: Int): Long

    @JvmStatic
    external fun read(handle: Long, buffer: ByteBuffer, length: Int): Long

    @JvmStatic
    external fun write(handle: Long, buffer: ByteBuffer, length: Int): Long

    @JvmStatic
    external fun cancel(handle: Long): Long

    @JvmStatic
    external fun close(handle: Long): Long

    @JvmStatic
    external fun clientProcessId(handle: Long): Long

    @JvmStatic
    external fun serverProcessId(handle: Long): Long

    @JvmStatic
    external fun processUserSid(processId: Long, buffer: ByteBuffer): Long
}
