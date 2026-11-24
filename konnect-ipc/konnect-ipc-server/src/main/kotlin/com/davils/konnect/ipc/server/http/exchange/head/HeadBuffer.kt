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

package com.davils.konnect.ipc.server.http.exchange.head

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString

private const val CR: Byte = 13
private const val LF: Byte = 10

internal class HeadBuffer(private val limit: Int) {
    private val bytes: Buffer = Buffer()

    private var lineLength: Int = 0

    private var isStarted: Boolean = false

    fun append(byte: Byte): HeadProgress {
        val isLeadingBreak = !isStarted && (byte == CR || byte == LF)
        if (isLeadingBreak) return HeadProgress.Pending
        return stored(byte)
    }

    fun outcome(progress: HeadProgress): HeadRead = when (progress) {
        HeadProgress.Complete -> HeadRead.Complete(head())
        HeadProgress.TooLarge -> HeadRead.TooLarge
        HeadProgress.Pending -> HeadRead.Ended
    }

    private fun head(): ByteString = bytes.readByteString()

    private fun stored(byte: Byte): HeadProgress {
        if (bytes.size >= limit) return HeadProgress.TooLarge
        bytes.writeByte(byte)
        isStarted = true
        return progressAfter(byte)
    }

    private fun progressAfter(byte: Byte): HeadProgress {
        if (byte != LF) {
            counted(byte)
            return HeadProgress.Pending
        }
        val isBlankLine = lineLength == 0
        lineLength = 0
        return HeadProgress.Complete.takeIf { isBlankLine } ?: HeadProgress.Pending
    }

    private fun counted(byte: Byte) {
        if (byte != CR) lineLength += 1
    }
}
