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

package com.davils.konnect.testing.ipc

import com.davils.konnect.ipc.core.transport.environment.IpcEnvironment
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableMap
import java.nio.file.Path

private val root: Path = Path.of("").toAbsolutePath().root

private val defaultTemporaryDirectory: Path = root.resolve("tmp")

/**
 * An environment with a fixed user, temporary directory and environment variables.
 *
 * @property userName the user the process runs as; [USER] by default.
 * @property temporaryDirectory the temporary directory; by default `tmp` under the root of the working directory,
 *   absolute on every operating system, such as `/tmp` or `C:\tmp`.
 * @param variables the environment variables.
 * @since 1.0.0
 */
@DavilsInternal
public class FakeIpcEnvironment(
    override val userName: String = USER,
    override val temporaryDirectory: Path = defaultTemporaryDirectory,
    variables: Map<String, String> = emptyMap(),
) : IpcEnvironment {
    private val variables: Map<String, String> = variables.immutableMap()

    override fun variable(name: String): String? = variables[name]

    /**
     * The default user.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The user of a fake environment unless another is given, `alice`.
         *
         * @since 1.0.0
         */
        public const val USER: String = "alice"
    }
}
