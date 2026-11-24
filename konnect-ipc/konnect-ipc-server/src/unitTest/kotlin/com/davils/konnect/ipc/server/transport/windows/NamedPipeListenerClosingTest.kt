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

package com.davils.konnect.ipc.server.transport.windows

import com.davils.konnect.ipc.core.transport.windows.PipeHandle
import com.davils.konnect.ipc.core.transport.windows.PipeIdentity
import com.davils.konnect.ipc.server.fixture.immediate
import com.davils.konnect.ipc.server.transport.AcceptOutcome
import com.davils.konnect.testing.ipc.FakeNamedPipeApi
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FIRST: Long = 1L
private const val BUFFER: Int = 1024
private const val NEXT: Long = 1_000L

class NamedPipeListenerClosingTest : FunSpec({
    context("a listener closed while a client connects") {
        test("gives up the next instance instead of keeping it") {
            val fake = FakeNamedPipeApi()
            var listener: NamedPipeListener? = null
            val api = ClosingOnConnect(fake) { listener?.close() }
            val created = NamedPipeListener(
                api = api,
                instances = PipeInstances(api = api, path = "pipe", securityDescriptor = "D:P", bufferSize = BUFFER),
                identity = PipeIdentity(api, FakeNamedPipeApi.CURRENT_PROCESS),
                first = PipeHandle(api, FIRST),
                dispatcher = immediate(),
            )
            listener = created

            created.accept().shouldBeInstanceOf<AcceptOutcome.Accepted>()

            fake.closed shouldContain NEXT
        }
    }
})
