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

package com.davils.konnect.ipc.core.access

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder behind [ipcAccess], which collects who may connect.
 *
 * A builder is not thread-safe; it is meant to be configured and evaluated by one thread, for one access. Once it
 * was evaluated by `build()` or `validate()`, every change throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IpcAccessBuilder @DavilsInternal constructor() : ValidatingBuilder<IpcAccess>() {
    override val subject: String get() = "ipc access"

    /**
     * Whether the user the process runs as may connect; `true` unless it is set.
     *
     * @since 1.0.0
     */
    public var isCurrentUserAllowed: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    internal val users: MutableSet<IpcUser> = linkedSetOf()

    /**
     * Lets [user] connect, too. Allowing a user twice allows it once.
     *
     * @param user the user to allow.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    public fun allow(user: IpcUser) {
        checkOpen()
        users += user
    }

    override fun assemble(): IpcAccess = IpcAccess(this)
}
