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

package com.davils.konnect.ipc.core.access

import com.davils.kore.core.annotation.DavilsInternal

/**
 * Builds an [IpcAccess] in a DSL, such as `ipcAccess { allow(IpcUser.UnixUser.ROOT) }`.
 *
 * The current user is allowed unless [IpcAccessBuilder.isCurrentUserAllowed] is set to `false`, so `ipcAccess {}`
 * equals [IpcAccess.CURRENT_USER].
 *
 * @param block the block that says who may connect.
 * @return the access.
 * @throws com.davils.kore.core.validation.report.ValidationException if the access lets nobody in; the report holds
 *   one error at `users` with the code [com.davils.konnect.ipc.core.IpcViolationCodes.NOBODY_ALLOWED].
 * @since 1.0.0
 */
public fun ipcAccess(block: IpcAccessBuilder.() -> Unit): IpcAccess {
    val builder = IpcAccessBuilder()
    builder.block()
    return builder.build()
}
