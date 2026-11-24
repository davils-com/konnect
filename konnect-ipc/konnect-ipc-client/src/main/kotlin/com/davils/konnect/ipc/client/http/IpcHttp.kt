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

package com.davils.konnect.ipc.client.http

import com.davils.kore.core.validation.validateOrThrow
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

private const val SUBJECT: String = "ipc http engine"

/**
 * The Ktor client engine that speaks HTTP/1.1 over local connections, such as `HttpClient(IpcHttp) { engine {
 * connector = ipcClient(Dispatchers.IO) { address = IpcAddress.UnixSocket(Path.of("/var/run/docker.sock")) } } }`.
 *
 * @since 1.0.0
 */
public object IpcHttp : HttpClientEngineFactory<IpcHttpConfig> {
    /**
     * Creates an engine from the configuration [block] declares.
     *
     * @param block declares the configuration.
     * @return the engine.
     * @throws com.davils.kore.core.validation.report.ValidationException if a bound of the configuration lies outside
     *   of its limits; the report names every problem and never the value it refused.
     * @since 1.0.0
     */
    override fun create(block: IpcHttpConfig.() -> Unit): HttpClientEngine {
        val config = IpcHttpConfig()
        config.block()
        return IpcHttpEngine(config.validateOrThrow(SUBJECT))
    }
}
