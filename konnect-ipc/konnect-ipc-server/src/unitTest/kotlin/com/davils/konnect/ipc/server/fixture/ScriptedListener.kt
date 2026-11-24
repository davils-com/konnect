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

import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.ipc.server.transport.IpcListener
import kotlinx.coroutines.awaitCancellation

internal class ScriptedListener(accepts: List<AcceptOutcome> = emptyList()) : IpcListener {
    private val pending = ArrayDeque(accepts)

    var closes: Int = 0
        private set

    fun add(outcome: AcceptOutcome) {
        pending += outcome
    }

    override suspend fun accept(): AcceptOutcome = pending.removeFirstOrNull() ?: awaitCancellation()

    override fun close() {
        closes += 1
    }
}
