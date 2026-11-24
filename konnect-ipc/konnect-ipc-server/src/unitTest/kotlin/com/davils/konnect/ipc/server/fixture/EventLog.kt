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

package com.davils.konnect.ipc.server.fixture

import com.davils.konfetti.core.EventBus
import com.davils.konfetti.testing.EventRecorder
import com.davils.konfetti.testing.testEventBus
import com.davils.konnect.ipc.server.IpcServerEvent
import com.davils.konnect.ipc.server.IpcServerTopics
import kotlinx.coroutines.CoroutineScope

private const val RECORDER: String = "recorder"
private const val DEFAULT_CAPACITY: Int = 64

internal class EventLog(scope: CoroutineScope, capacity: Int = DEFAULT_CAPACITY) {
    private val recorder = EventRecorder<IpcServerEvent>()

    val bus: EventBus = testEventBus(scope) {
        topic(IpcServerTopics.EVENTS) { bufferCapacity = capacity }
    }

    fun record() {
        bus[IpcServerTopics.EVENTS].consume(RECORDER) { handle(recorder::record) }.getOrThrow()
    }

    suspend fun events(count: Int): List<IpcServerEvent> {
        val envelopes = recorder.awaitCount(count)
        return envelopes.map { envelope -> envelope.payload }
    }
}
