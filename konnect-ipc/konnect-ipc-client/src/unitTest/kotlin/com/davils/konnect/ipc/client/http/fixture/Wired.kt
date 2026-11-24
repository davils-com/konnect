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

package com.davils.konnect.ipc.client.http.fixture

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

internal suspend fun <T> wired(block: suspend CoroutineScope.(Wire) -> T): T {
    val scope = CoroutineScope(SupervisorJob())
    return try {
        withTimeout(10.seconds) { scope.block(Wire(scope)) }
    } finally {
        scope.cancel()
    }
}

internal suspend fun failureOf(block: suspend () -> Unit): IOException? = try {
    block()
    null
} catch (failure: IOException) {
    failure
}

internal fun Throwable.causes(): List<Throwable> = generateSequence(this) { failure -> failure.cause }.toList()
