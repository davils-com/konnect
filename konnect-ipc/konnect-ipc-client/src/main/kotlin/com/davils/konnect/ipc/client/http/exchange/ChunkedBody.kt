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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.readByte
import kotlinx.io.Buffer
import kotlinx.io.readString
import java.io.IOException

private const val CR: Byte = 13
private const val LF: Byte = 10
private const val HEX: Int = 16
private const val EXTENSION: Char = ';'
private val chunkSize: Regex = Regex("[0-9A-Fa-f]{1,15}")

internal class ChunkedBody(private val maxTrailerSize: Int) {
    suspend fun copy(input: ByteReadChannel, output: ByteWriteChannel) {
        var size = sizeOf(input)
        while (size > 0L) {
            val copied = input.copyTo(output, size)
            if (copied < size) throw IOException(BODY_CUT_OFF)
            lineEnd(input)
            size = sizeOf(input)
        }
        trailers(input)
    }

    private suspend fun sizeOf(input: ByteReadChannel): Long {
        val line = lineOf(input, Limits.MAX_CHUNK_LINE_LENGTH, MALFORMED_BODY)
        val digits = line.substringBefore(EXTENSION).trim()
        if (!chunkSize.matches(digits)) throw IOException(MALFORMED_BODY)
        return digits.toLong(HEX)
    }

    private suspend fun trailers(input: ByteReadChannel) {
        var remaining = maxTrailerSize
        var line = lineOf(input, remaining, TRAILERS_TOO_LARGE)
        while (line.isNotEmpty()) {
            remaining -= line.length
            line = lineOf(input, remaining, TRAILERS_TOO_LARGE)
        }
    }

    private suspend fun lineEnd(input: ByteReadChannel) {
        val first = nextByte(input)
        val second = nextByte(input)
        if (first != CR || second != LF) throw IOException(MALFORMED_BODY)
    }

    private suspend fun lineOf(input: ByteReadChannel, maxLength: Int, tooLong: String): String = Buffer().use { line ->
        var byte = nextByte(input)
        while (byte != CR) {
            if (byte == LF) throw IOException(MALFORMED_BODY)
            if (line.size >= maxLength) throw IOException(tooLong)
            line.writeByte(byte)
            byte = nextByte(input)
        }
        if (nextByte(input) != LF) throw IOException(MALFORMED_BODY)
        line.readString(Charsets.ISO_8859_1)
    }

    private suspend fun nextByte(input: ByteReadChannel): Byte {
        if (!input.awaitContent()) throw IOException(BODY_CUT_OFF)
        return input.readByte()
    }
}
