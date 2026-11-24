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

package com.davils.konnect.ipc.server.http.fixture

import java.io.ByteArrayOutputStream
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path

private const val BUFFER: Int = 4096

internal class RawSocket(path: Path) : AutoCloseable {
    private val channel: SocketChannel = SocketChannel.open(UnixDomainSocketAddress.of(path))

    fun send(text: String) {
        val buffer = ByteBuffer.wrap(text.encodeToByteArray())
        while (buffer.hasRemaining()) channel.write(buffer)
    }

    fun readUntil(marker: String): String {
        val received = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(1)
        while (!received.toString(Charsets.UTF_8).endsWith(marker) && channel.read(buffer) >= 0) {
            received.write(buffer.array(), 0, buffer.position())
            buffer.clear()
        }
        return received.toString(Charsets.UTF_8)
    }

    fun rest(): String {
        val received = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(BUFFER)
        while (channel.read(buffer) >= 0) {
            received.write(buffer.array(), 0, buffer.position())
            buffer.clear()
        }
        return received.toString(Charsets.UTF_8)
    }

    override fun close() {
        channel.close()
    }
}
