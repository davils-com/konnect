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

package com.davils.konnect.ipc.core.connection

private const val HASH_FACTOR: Int = 31
private const val NO_CODE: String = "none"

/**
 * A failure the operating system reported for a connection.
 *
 * The description is constant text chosen by Konnect, such as `Win32 error 109` or the simple name of the exception
 * of the JDK, and never carries a message of the operating system or of the JDK, because those may quote paths and
 * names that came from outside. Instances are immutable.
 *
 * @property code the error code of the operating system, such as a Win32 error, or `null` if the platform reported
 *   none.
 * @property description what failed, in constant text.
 * @since 1.0.0
 */
public class IpcSystemError(public val code: Long?, public val description: String) {
    /**
     * Tells whether [other] is an error with the same code and description.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal error.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is IpcSystemError && code == other.code && description == other.description

    /**
     * Computes a hash code from the code and the description.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * code.hashCode() + description.hashCode()

    /**
     * Shows the code and the description.
     *
     * @return the error as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcSystemError(code=${code ?: NO_CODE}, description=$description)"
}
