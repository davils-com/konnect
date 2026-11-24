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

package com.davils.konnect.ipc.core.framing

import com.davils.konnect.ipc.core.Limits
import com.davils.konnect.ipc.core.connection.IpcConnection
import kotlinx.io.bytestring.ByteString

/**
 * How a stream of bytes is split into frames of type [F].
 *
 * An implementation reads from a connection that may come from an attacker: it checks every size the other side
 * announces against its own limit before it reads or allocates anything for it, and closes the connection when a
 * frame is refused.
 *
 * @param F the type of a frame.
 * @since 1.0.0
 */
public interface IpcFraming<F> {
    /**
     * Receives the next frame from [connection].
     *
     * @param connection the connection to read from.
     * @return the frame, or why there is none.
     * @since 1.0.0
     */
    public suspend fun receive(connection: IpcConnection): IpcFrameResult<F>

    /**
     * Encodes [frame] into the bytes that carry it.
     *
     * @param frame the frame to encode.
     * @return the bytes.
     * @throws IllegalArgumentException if the frame exceeds the limit of this framing.
     * @since 1.0.0
     */
    public fun encode(frame: F): ByteString

    /**
     * The framings Konnect provides.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Frames that carry their size as four bytes in network order in front of their bytes.
         *
         * A frame that announces more than [maxFrameSize] bytes is refused with [IpcFrameResult.TooLarge] before
         * any byte of its body is read, and the connection is closed. Instances are immutable and may be shared.
         *
         * @param maxFrameSize the largest frame received or encoded, in bytes; [Limits.DEFAULT_MAX_FRAME_SIZE] by
         *   default.
         * @return the framing.
         * @throws IllegalArgumentException if [maxFrameSize] is not positive.
         * @since 1.0.0
         */
        public fun lengthPrefixed(maxFrameSize: Int = Limits.DEFAULT_MAX_FRAME_SIZE): IpcFraming<ByteString> =
            LengthPrefixedFraming(maxFrameSize)
    }
}
