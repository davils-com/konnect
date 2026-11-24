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

package com.davils.konnect.ipc.server.http.call

import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.server.http.exchange.request.RequestHead
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.URLProtocol

private const val PORT: Char = ':'
private const val LOCALHOST: String = "localhost"
private const val UNKNOWN: String = "unknown"
private const val PROCESS: String = "process:"

internal class IpcConnectionPoint(head: RequestHead, origin: CallOrigin) : RequestConnectionPoint {
    private val hostHeader: String? = head.headers[HttpHeaders.Host]

    override val scheme: String = URLProtocol.HTTP.name

    override val version: String = head.version

    override val uri: String = head.uri

    override val method: HttpMethod = head.method

    override val localPort: Int = URLProtocol.HTTP.defaultPort

    override val serverPort: Int = portOf(hostHeader) ?: localPort

    override val localHost: String = LOCALHOST

    override val serverHost: String = hostOf(hostHeader) ?: localHost

    override val localAddress: String = origin.address.toString()

    override val remoteHost: String = userOf(origin.peer.user)

    override val remotePort: Int = 0

    override val remoteAddress: String = processOf(origin.peer.processId)

    @Deprecated("Use localPort or serverPort instead", level = DeprecationLevel.ERROR)
    override val port: Int get() = localPort

    @Deprecated("Use localHost or serverHost instead", level = DeprecationLevel.ERROR)
    override val host: String get() = localHost

    override fun toString(): String = "IpcConnectionPoint(method=$method, localAddress=$localAddress, " +
        "remoteHost=$remoteHost, remoteAddress=$remoteAddress)"

    private fun hostOf(host: String?): String? {
        val value = host ?: return null
        return value.substringBeforeLast(PORT)
    }

    private fun userOf(user: IpcUser?): String {
        val known = user ?: return UNKNOWN
        return known.toString()
    }

    private fun processOf(process: Long?): String {
        val known = process ?: return UNKNOWN
        return "$PROCESS$known"
    }

    private fun portOf(host: String?): Int? {
        val port = host?.substringAfterLast(PORT, missingDelimiterValue = "")
        return port?.toIntOrNull()
    }
}
