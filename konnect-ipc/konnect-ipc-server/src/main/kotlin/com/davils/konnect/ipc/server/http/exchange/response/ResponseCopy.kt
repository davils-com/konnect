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

package com.davils.konnect.ipc.server.http.exchange.response

import io.ktor.http.cio.encodeChunked
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.discard

internal object ResponseCopy {
    suspend fun copy(framing: ResponseFraming, body: ByteReadChannel, output: ByteWriteChannel) {
        when (framing) {
            ResponseFraming.Fixed, ResponseFraming.UntilClose -> body.copyTo(output)
            ResponseFraming.Chunked -> encodeChunked(output, body)
            ResponseFraming.Discarded -> body.discard()
        }
        output.flush()
    }
}
