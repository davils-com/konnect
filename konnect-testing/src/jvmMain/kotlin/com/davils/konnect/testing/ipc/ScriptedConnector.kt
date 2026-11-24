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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.client.IpcConnectResult
import com.davils.konnect.ipc.client.IpcConnector
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A connector that answers with the results it was given, for tests of code that connects through an
 * [IpcConnector], such as the HTTP engine of Konnect.
 *
 * Each attempt takes the next of the results, in order; once they are used up, every attempt answers with the
 * exhausted result. A [IpcConnectResult.Connected] hands out the same connection every time it is answered. It is
 * safe to use from several coroutines.
 *
 * @param results the results of the attempts, in order.
 * @param exhausted the result of every attempt after the last; [IpcConnectResult.NotFound] unless it is given.
 * @since 1.0.0
 */
public class ScriptedConnector(
    results: List<IpcConnectResult>,
    private val exhausted: IpcConnectResult = IpcConnectResult.NotFound,
) : IpcConnector {
    private val script: List<IpcConnectResult> = results.immutableList()

    private val calls = AtomicInt(0)

    /**
     * How many attempts there were.
     *
     * @since 1.0.0
     */
    public val attempts: Int get() = calls.load()

    override suspend fun connect(): IpcConnectResult {
        val index = calls.fetchAndAdd(1)
        return script.getOrElse(index) { exhausted }
    }

    /**
     * Shows how many results are scripted and how many attempts there were.
     *
     * @return the connector as text.
     * @since 1.0.0
     */
    override fun toString(): String = "ScriptedConnector(results=${script.size}, attempts=$attempts)"
}
