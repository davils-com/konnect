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

private const val HASH_FACTOR: Int = 31

/**
 * The pipe an address leads to, in a session.
 *
 * @property session the session the path was found in.
 * @property path the full path of the pipe.
 * @since 1.0.0
 */
@DavilsInternal
public class PipeTarget(public val session: PipeSession, public val path: String) {
    /**
     * Tells whether [other] is the same pipe of the same session.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal target.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is PipeTarget && session == other.session && path == other.path

    /**
     * Computes a hash code from the session and the path.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * session.hashCode() + path.hashCode()

    /**
     * Shows the path.
     *
     * @return the target as text.
     * @since 1.0.0
     */
    override fun toString(): String = "PipeTarget(path=$path)"
}
