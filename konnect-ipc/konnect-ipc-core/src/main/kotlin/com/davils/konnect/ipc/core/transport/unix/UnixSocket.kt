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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.kore.core.annotation.DavilsInternal
import java.nio.ByteBuffer

/**
 * A connected Unix domain socket, as the JDK offers it.
 *
 * Every function blocks the calling thread and may throw the [java.io.IOException] of the JDK.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface UnixSocket {
    /**
     * Reads the name of the user the process on the other side runs as.
     *
     * @return the user name.
     * @throws java.io.IOException if the credentials cannot be read.
     * @throws UnsupportedOperationException if the platform offers no peer credentials.
     * @since 1.0.0
     */
    public fun peerUser(): String

    /**
     * Reads into [buffer].
     *
     * @param buffer the buffer to fill from its position.
     * @return how many bytes were read, or `-1` at the end of the stream.
     * @since 1.0.0
     */
    public fun read(buffer: ByteBuffer): Int

    /**
     * Writes from [buffer].
     *
     * @param buffer the buffer to write from its position.
     * @return how many bytes were written.
     * @since 1.0.0
     */
    public fun write(buffer: ByteBuffer): Int

    /**
     * Shuts the output down, so the other side reads its end.
     *
     * @since 1.0.0
     */
    public fun shutdownOutput()

    /**
     * Closes the socket.
     *
     * @since 1.0.0
     */
    public fun close()
}
