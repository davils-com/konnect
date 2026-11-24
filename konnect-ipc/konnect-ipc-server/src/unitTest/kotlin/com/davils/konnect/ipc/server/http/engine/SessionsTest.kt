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

package com.davils.konnect.ipc.server.http.engine

import com.davils.konnect.ipc.server.fixture.SERVICE
import com.davils.konnect.ipc.server.http.connection.ConnectionSession
import com.davils.konnect.ipc.server.http.connection.SessionContext
import com.davils.konnect.testing.ipc.TestIpc
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.engine.EnginePipeline
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.seconds

private fun session(): ConnectionSession {
    val context = SessionContext(
        idleTimeout = 1.seconds,
        requestBodyTimeout = 1.seconds,
        maxHeaderSize = 1,
        maxRequestBodySize = 1,
        maxRequestsPerConnection = 1,
        telemetry = EngineTelemetry(registry = null, address = SERVICE),
        address = SERVICE,
        pipeline = EnginePipeline(developmentMode = false),
        application = { error("no application is needed") },
    )
    return ConnectionSession(TestIpc.connections().server, context)
}

class SessionsTest : FunSpec({
    context("sessions") {
        test("that arrive while they drain are drained at once and leave a busy session running") {
            val sessions = Sessions()
            val job = Job()
            sessions.drain()

            sessions.add(job, session())

            sessions.size shouldBe 1
            job.isCancelled shouldBe false
            sessions.remove(job)
            sessions.size shouldBe 0
        }
    }
})
