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

import com.davils.konnect.ipc.server.http.exchange.request.RequestHead
import com.davils.kore.core.failure.contained
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.parseQueryString
import io.ktor.server.application.PipelineCall
import io.ktor.server.engine.BaseApplicationRequest
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.RequestCookies
import io.ktor.utils.io.ByteReadChannel

private val valueless: List<String> = listOf("")
private const val MALFORMED_QUERY: String = "The query of the request is malformed"

internal class IpcApplicationRequest(
    call: PipelineCall,
    private val head: RequestHead,
    body: ByteReadChannel,
    origin: CallOrigin,
) : BaseApplicationRequest(call) {
    override val engineHeaders: Headers = head.headers

    override val engineReceiveChannel: ByteReadChannel = body

    override val local: RequestConnectionPoint = IpcConnectionPoint(head, origin)

    override val cookies: RequestCookies by lazy { RequestCookies(this) }

    override val rawQueryParameters: Parameters by lazy { parseQueryString(head.query, decode = false) }

    override val queryParameters: Parameters by lazy { decodedQuery() }

    private fun decodedQuery(): Parameters {
        val parsed = contained { parseQueryString(head.query) }.getOrElse { throw BadRequestException(MALFORMED_QUERY) }
        return Parameters.build {
            parsed.forEach { name, values -> appendAll(name, values.ifEmpty { valueless }) }
        }
    }
}
