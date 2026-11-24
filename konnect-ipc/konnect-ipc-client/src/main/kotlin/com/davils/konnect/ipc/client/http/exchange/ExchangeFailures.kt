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

internal const val CLOSED_BEFORE_ANSWER: String = "The server closed the connection before it answered"

internal const val HEAD_TOO_LARGE: String = "The head of the response exceeds its limit"

internal const val HEAD_CUT_OFF: String = "The server closed the connection in the middle of the head of its response"

internal const val MALFORMED_HEAD: String = "The head of the response is malformed"

internal const val TOO_MANY_INTERIM: String = "The server sent too many interim responses"

internal const val BODY_CUT_OFF: String =
    "The server closed the connection before the body of its response was complete"

internal const val MALFORMED_BODY: String = "The chunks of the body of the response are malformed"

internal const val TRAILERS_TOO_LARGE: String = "The trailers of the response exceed their limit"

internal const val LINE_BREAK_IN_HEADER: String = "A header of the request holds a line break"

internal const val FOREIGN_UPGRADE: String =
    "A protocol upgrade of Ktor is not sent over local connections; use IpcHttpUpgradeCapability"
