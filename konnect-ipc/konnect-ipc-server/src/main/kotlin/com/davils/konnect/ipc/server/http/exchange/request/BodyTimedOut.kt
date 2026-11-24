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

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.davils.konnect.ipc.server.http.exchange.request

import io.ktor.util.cio.ChannelIOException
import kotlinx.coroutines.CopyableThrowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.IOException

private const val TIMED_OUT: String = "The body of the request did not arrive in time"

internal class BodyTimedOut :
    ChannelIOException(TIMED_OUT, IOException(TIMED_OUT)),
    CopyableThrowable<BodyTimedOut> {
    override fun createCopy(): BodyTimedOut = BodyTimedOut()
}
