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

package com.davils.konnect.ipc.core.transport.windows

import com.davils.kore.core.annotation.DavilsInternal
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * An open pipe handle that is closed only once no call uses it any more.
 *
 * [close] cancels every pending operation at once and releases the handle when the last call that uses it has
 * left, so a handle value is never reused by Windows while a call still holds it. It is safe to use from several
 * threads.
 *
 * @param api the pipe operations that cancel and close the handle.
 * @property value the handle.
 * @since 1.0.0
 */
@DavilsInternal
@OptIn(ExperimentalAtomicApi::class)
public class PipeHandle(private val api: PipeIoApi, public val value: Long) {
    private val users = AtomicInt(0)

    private val closing = AtomicBoolean(false)

    private val released = AtomicBoolean(false)

    /**
     * Whether the handle was not closed yet.
     *
     * @since 1.0.0
     */
    public val isOpen: Boolean get() = !closing.load()

    /**
     * Runs [call] with the handle, unless it is closed.
     *
     * @param T the type of the result.
     * @param ifClosed what to return when the handle is closed.
     * @param call the call that uses the handle.
     * @return what [call] returned, or [ifClosed].
     * @since 1.0.0
     */
    public suspend fun <T> use(ifClosed: T, call: suspend (Long) -> T): T {
        if (!entered()) return ifClosed
        try {
            return call(value)
        } finally {
            left()
        }
    }

    /**
     * Cancels every pending operation and releases the handle once no call uses it; closing it again does nothing.
     *
     * @since 1.0.0
     */
    public fun close() {
        if (!closing.compareAndSet(expectedValue = false, newValue = true)) return
        api.cancel(value)
        if (users.load() == 0) release()
    }

    private fun entered(): Boolean {
        users.addAndFetch(1)
        if (!closing.load()) return true
        left()
        return false
    }

    private fun left() {
        val remaining = users.addAndFetch(-1)
        if (remaining == 0 && closing.load()) release()
    }

    private fun release() {
        if (!released.compareAndSet(expectedValue = false, newValue = true)) return
        api.close(value)
    }
}
