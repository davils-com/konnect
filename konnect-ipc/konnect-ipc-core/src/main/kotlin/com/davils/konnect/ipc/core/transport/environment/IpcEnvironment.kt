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

package com.davils.konnect.ipc.core.transport.environment

import com.davils.kore.core.annotation.DavilsInternal
import java.nio.file.Path

/**
 * What the transports read from the process they run in.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface IpcEnvironment {
    /**
     * The name of the user the process runs as.
     *
     * @since 1.0.0
     */
    public val userName: String

    /**
     * The temporary directory of the process.
     *
     * @since 1.0.0
     */
    public val temporaryDirectory: Path

    /**
     * Reads the environment variable [name].
     *
     * @param name the name of the variable.
     * @return its value, or `null` if it is not set.
     * @since 1.0.0
     */
    public fun variable(name: String): String?
}
