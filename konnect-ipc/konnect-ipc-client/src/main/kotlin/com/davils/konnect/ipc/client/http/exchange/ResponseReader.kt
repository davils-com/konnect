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

package com.davils.konnect.ipc.client.http.exchange

import com.davils.konnect.ipc.client.Limits
import com.davils.kore.core.failure.contained
import io.ktor.http.cio.parseResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByte
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import java.io.IOException

private const val CR: Byte = 13
private const val LF: Byte = 10
private val terminator: ByteArray = byteArrayOf(CR, LF, CR, LF)

internal class ResponseReader(private val maxHeadSize: Int) {
    suspend fun head(input: ByteReadChannel): ResponseHead {
        repeat(Limits.MAX_INTERIM_RESPONSES + 1) {
            val head = parsed(input)
            if (!head.isInterim) return head
        }
        throw IOException(TOO_MANY_INTERIM)
    }

    private suspend fun parsed(input: ByteReadChannel): ResponseHead {
        val bytes = headBytes(input)
        val head = contained { checkNotNull(parseResponse(ByteReadChannel(bytes))).use(ResponseHeads::of) }
        return head.getOrNull() ?: throw IOException(MALFORMED_HEAD)
    }

    private suspend fun headBytes(input: ByteReadChannel): ByteArray = Buffer().use { head ->
        var matched = 0
        while (matched < terminator.size) {
            if (head.size >= maxHeadSize) throw IOException(HEAD_TOO_LARGE)
            if (!input.awaitContent()) throw IOException(cutOff(head.size))
            val byte = input.readByte()
            head.writeByte(byte)
            matched = progress(matched, byte)
        }
        head.readByteArray()
    }

    private fun progress(matched: Int, byte: Byte): Int {
        var next = 0
        if (byte == CR) next = 1
        if (byte == terminator[matched]) next = matched + 1
        return next
    }

    private fun cutOff(size: Long): String {
        if (size == 0L) return CLOSED_BEFORE_ANSWER
        return HEAD_CUT_OFF
    }
}
