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

package com.davils.konnect.ipc.server.http.exchange

import com.davils.konnect.ipc.server.http.call.CallOrigin
import com.davils.konnect.ipc.server.http.call.IpcApplicationCall
import com.davils.konnect.ipc.server.http.connection.ConnectionWire
import com.davils.konnect.ipc.server.http.connection.SessionContext
import com.davils.konnect.ipc.server.http.exchange.request.Admission
import com.davils.konnect.ipc.server.http.exchange.request.BodyFraming
import com.davils.konnect.ipc.server.http.exchange.request.RequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job

internal class Exchange(
    private val admission: Admission.Admitted,
    private val wire: ConnectionWire,
    private val context: SessionContext,
    private val origin: CallOrigin,
) {
    private val head = admission.head

    suspend fun served(isClosing: () -> Boolean): Boolean = coroutineScope {
        val jobs = SupervisorJob(coroutineContext.job)
        try {
            servedWithin(CoroutineScope(coroutineContext + jobs), isClosing)
        } finally {
            jobs.cancel()
        }
    }

    private suspend fun servedWithin(scope: CoroutineScope, isClosing: () -> Boolean): Boolean {
        if (head.isContinueExpected && admission.framing != BodyFraming.Empty) wire.sendContinue()
        val body = RequestBody.start(
            framing = admission.framing,
            input = wire.input,
            scope = scope,
            limit = context.maxRequestBodySize,
            timeout = context.requestBodyTimeout,
        )
        val exchange = ExchangeWire(
            input = wire.input,
            output = wire.output,
            scope = scope,
            isHead = head.isHead,
            closing = { !head.isPersistent || isClosing() },
        )
        val call = IpcApplicationCall(
            application = context.application(),
            head = head,
            body = body.channel,
            wire = exchange,
            origin = origin,
        )
        context.pipeline.execute(call, Unit)
        val isDrained = body.finish()
        if (body.isTimedOut) return timedOut(call)
        context.telemetry.served()
        return isDrained && call.response.isReusable
    }

    private suspend fun timedOut(call: IpcApplicationCall): Boolean {
        context.telemetry.rejected(Rejection.BodyTimeout)
        if (!call.response.isCommitted) Rejection.BodyTimeout.write(wire.output)
        return false
    }
}
