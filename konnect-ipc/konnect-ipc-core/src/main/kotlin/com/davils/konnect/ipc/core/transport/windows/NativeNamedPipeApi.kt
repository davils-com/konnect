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
import com.davils.kore.core.platform.NativeResult
import java.nio.ByteBuffer

@DavilsPlatformBinding
internal object NativeNamedPipeApi : NamedPipeApi {
    override fun createServer(
        path: String,
        securityDescriptor: String,
        bufferSize: Int,
        isFirstInstance: Boolean,
    ): NativeResult = NativeResult.fromEncoded(
        NamedPipeNative.createServer(
            path = path,
            securityDescriptor = securityDescriptor,
            bufferSize = bufferSize,
            isFirstInstance = isFirstInstance,
        ),
    )

    override fun awaitClient(handle: Long): NativeResult = NativeResult.fromEncoded(NamedPipeNative.awaitClient(handle))

    override fun open(path: String): NativeResult = NativeResult.fromEncoded(NamedPipeNative.open(path))

    override fun waitAvailable(path: String, timeoutMillis: Int): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.waitAvailable(path, timeoutMillis))

    override fun read(handle: Long, buffer: ByteBuffer, length: Int): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.read(handle, buffer, length))

    override fun write(handle: Long, buffer: ByteBuffer, length: Int): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.write(handle, buffer, length))

    override fun cancel(handle: Long): NativeResult = NativeResult.fromEncoded(NamedPipeNative.cancel(handle))

    override fun close(handle: Long): NativeResult = NativeResult.fromEncoded(NamedPipeNative.close(handle))

    override fun clientProcessId(handle: Long): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.clientProcessId(handle))

    override fun serverProcessId(handle: Long): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.serverProcessId(handle))

    override fun processUserSid(processId: Long, buffer: ByteBuffer): NativeResult =
        NativeResult.fromEncoded(NamedPipeNative.processUserSid(processId, buffer))
}
