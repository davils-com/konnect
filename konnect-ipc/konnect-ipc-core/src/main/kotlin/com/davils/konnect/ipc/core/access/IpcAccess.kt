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

import com.davils.konnect.ipc.core.IpcViolationCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val HASH_FACTOR: Int = 31

/**
 * Who may connect to a local server: the current user, a list of other users, or both.
 *
 * An access is immutable and safe to share between threads. A peer whose user is unknown is never let in.
 *
 * @since 1.0.0
 */
public class IpcAccess internal constructor(builder: IpcAccessBuilder) : Validatable {
    /**
     * Whether the user the process runs as may connect.
     *
     * @since 1.0.0
     */
    public val isCurrentUserAllowed: Boolean = builder.isCurrentUserAllowed

    /**
     * The other users that may connect, in the order they were allowed, without duplicates.
     *
     * @since 1.0.0
     */
    public val users: List<IpcUser> = builder.users.immutableList()

    /**
     * Whether only the current user may connect.
     *
     * @since 1.0.0
     */
    public val isCurrentUserOnly: Boolean get() = isCurrentUserAllowed && users.isEmpty()

    /**
     * Tells whether a peer running as [user] may connect.
     *
     * @param user the user of the peer, or `null` if it could not be read.
     * @param currentUser the user the process runs as, or `null` if it could not be read.
     * @return `true` if [user] is known and either the current user, when it is allowed, or one of [users].
     * @since 1.0.0
     */
    public fun permits(user: IpcUser?, currentUser: IpcUser?): Boolean {
        if (user == null) return false
        val isAllowedCurrentUser = isCurrentUserAllowed && user == currentUser
        return isAllowedCurrentUser || user in users
    }

    /**
     * Reports an access that lets nobody in with the code [IpcViolationCodes.NOBODY_ALLOWED].
     *
     * @receiver the validation that collects the violation.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (isCurrentUserAllowed || users.isNotEmpty()) return
        fail(
            property = ::users,
            message = "must name somebody when the current user is not allowed",
            code = IpcViolationCodes.NOBODY_ALLOWED,
        )
    }

    /**
     * Tells whether [other] is an access that lets in the same users.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal access.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is IpcAccess && isCurrentUserAllowed == other.isCurrentUserAllowed && users == other.users

    /**
     * Computes a hash code from who is let in.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * isCurrentUserAllowed.hashCode() + users.hashCode()

    /**
     * Shows who is let in.
     *
     * @return the access as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcAccess(isCurrentUserAllowed=$isCurrentUserAllowed, users=$users)"

    /**
     * The default access.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The access that lets in the current user and nobody else.
         *
         * @since 1.0.0
         */
        public val CURRENT_USER: IpcAccess = IpcAccess(IpcAccessBuilder())
    }
}
