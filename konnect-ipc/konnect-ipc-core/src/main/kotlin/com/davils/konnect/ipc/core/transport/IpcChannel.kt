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

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.connection.IpcWriteResult
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The bytes of one connection, as a transport of the operating system or of memory moves them.
 *
 * A channel is driven by one [com.davils.konnect.ipc.core.connection.IpcConnection], which never reads twice or
 * writes twice at the same time; [close] may be called from any thread at any time and ends a pending read or write.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface IpcChannel {
    /**
     * The process on the other side.
     *
     * @since 1.0.0
     */
    public val peer: IpcPeer

    /**
     * Reads at most [length] bytes into [target] at [offset], suspending until at least one byte arrived.
     *
     * @param target the array to read into.
     * @param offset the first index of [target] to fill.
     * @param length the most bytes to read; at least 1.
     * @return how many bytes were read, the end of the stream, or a failure.
     * @since 1.0.0
     */
    public suspend fun read(target: ByteArray, offset: Int, length: Int): IpcChannelRead

    /**
     * Writes all [length] bytes of [source] from [offset].
     *
     * @param source the array to write from.
     * @param offset the first index of [source] to write.
     * @param length how many bytes to write.
     * @return whether every byte was written.
     * @since 1.0.0
     */
    public suspend fun write(source: ByteArray, offset: Int, length: Int): IpcWriteResult

    /**
     * Closes the output, so the other side reads its end.
     *
     * @return whether the output was closed.
     * @since 1.0.0
     */
    public suspend fun closeOutput(): IpcWriteResult

    /**
     * Closes the channel and releases what it holds of the operating system.
     *
     * @since 1.0.0
     */
    public fun close()
}
