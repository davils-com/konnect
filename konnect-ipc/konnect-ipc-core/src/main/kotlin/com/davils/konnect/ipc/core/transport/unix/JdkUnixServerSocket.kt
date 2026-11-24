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

package com.davils.konnect.ipc.core.transport.unix

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

@DavilsPlatformBinding
internal class JdkUnixServerSocket(private val channel: ServerSocketChannel) : UnixServerSocket {
    private val selector: Selector = Selector.open()

    init {
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_ACCEPT)
    }

    override fun accept(): UnixSocket? {
        selector.select()
        val selected = selector.selectedKeys()
        selected.clear()
        val accepted = channel.accept() ?: return null
        return JdkUnixSocket(accepted)
    }

    override fun wakeup() {
        selector.wakeup()
    }

    override fun close() {
        selector.close()
        channel.close()
    }
}
