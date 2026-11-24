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

package com.davils.konnect.ipc.server.http.exchange.fixture

import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8

internal val untilClose: OutgoingContent = object : OutgoingContent.WriteChannelContent() {
    override val contentType: ContentType = ContentType.Text.Plain

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeStringUtf8("until close")
    }
}

internal val broken: OutgoingContent = object : OutgoingContent.WriteChannelContent() {
    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeStringUtf8("partial")
        channel.flush()
        error("the application failed while it wrote")
    }
}
