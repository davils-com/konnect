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

package com.davils.konnect.testing.ipc

import com.davils.kore.core.annotation.DavilsInternal
import java.util.Objects

/**
 * A server pipe a [FakeNamedPipeApi] was asked to create.
 *
 * @property path the full path of the pipe.
 * @property securityDescriptor the security descriptor in SDDL.
 * @property isFirstInstance whether the pipe had to be the first instance of its name.
 * @since 1.0.0
 */
@DavilsInternal
public class FakeCreatedPipe(
    public val path: String,
    public val securityDescriptor: String,
    public val isFirstInstance: Boolean,
) {
    /**
     * Tells whether [other] is the same request.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal request.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is FakeCreatedPipe &&
        path == other.path &&
        securityDescriptor == other.securityDescriptor &&
        isFirstInstance == other.isFirstInstance

    /**
     * Computes a hash code from the path, the descriptor and the instance flag.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = Objects.hash(path, securityDescriptor, isFirstInstance)

    /**
     * Shows the request.
     *
     * @return the request as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "FakeCreatedPipe(path=$path, securityDescriptor=$securityDescriptor, isFirstInstance=$isFirstInstance)"
}
