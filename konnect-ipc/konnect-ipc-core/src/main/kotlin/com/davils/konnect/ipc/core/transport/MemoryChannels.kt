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
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.channels.Channel

/**
 * Creates two channels that carry bytes to each other in memory, for tests.
 *
 * What one channel writes the other reads, in as many reads as the reader asks for. Closing the output of one ends
 * what the other reads; closing one ends both. Writes never wait, so the channels buffer without a bound.
 *
 * @param first the process the second channel sees on the other side.
 * @param second the process the first channel sees on the other side.
 * @return the channel of [first] and the channel of [second].
 * @since 1.0.0
 */
@DavilsInternal
public fun memoryChannels(first: IpcPeer, second: IpcPeer): Pair<IpcChannel, IpcChannel> {
    val towardsFirst = Channel<ByteArray>(Channel.UNLIMITED)
    val towardsSecond = Channel<ByteArray>(Channel.UNLIMITED)
    val seenByFirst = MemoryChannel(peer = second, incoming = towardsFirst, outgoing = towardsSecond)
    val seenBySecond = MemoryChannel(peer = first, incoming = towardsSecond, outgoing = towardsFirst)
    return seenByFirst to seenBySecond
}
