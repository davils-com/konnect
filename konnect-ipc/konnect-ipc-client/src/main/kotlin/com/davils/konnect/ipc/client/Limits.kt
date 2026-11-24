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

package com.davils.konnect.ipc.client

/**
 * The bounds the client of local connections and its HTTP engine enforce on what they hold and what a server sends.
 *
 * Every bound is checked before the work that grows with it: a configuration outside of its bounds is refused when the
 * client or the engine is created, and a response that exceeds one is refused while it is read, with an exception
 * whose message is constant text and never quotes what the server sent. Nothing is truncated silently.
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one is a breaking change.
 * Each limit is a compile-time constant, so code that reads one keeps the value it was compiled against until it is
 * compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The most connections an HTTP engine has open at the same time unless another number is given: 16.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_CONNECTIONS: Int = 16

    /**
     * The largest number of connections an HTTP engine may have open at the same time: 1024.
     *
     * @since 1.0.0
     */
    public const val MAX_CONNECTIONS: Int = 1024

    /**
     * The most idle connections an HTTP engine keeps for the next request unless another number is given: 4.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_IDLE_CONNECTIONS: Int = 4

    /**
     * The largest number of idle connections an HTTP engine may keep: 1024.
     *
     * @since 1.0.0
     */
    public const val MAX_IDLE_CONNECTIONS: Int = 1024

    /**
     * How long an HTTP engine keeps an idle connection unless another time is given, in milliseconds: 30 seconds.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_IDLE_TIMEOUT_MILLIS: Long = 30_000L

    /**
     * The smallest limit of the head of a response, its status line and its headers, in bytes: 1 KiB.
     *
     * @since 1.0.0
     */
    public const val MIN_RESPONSE_HEAD_SIZE: Int = 1024

    /**
     * The limit of the head of a response unless another is given, in bytes: 32 KiB.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_RESPONSE_HEAD_SIZE: Int = 32 * 1024

    /**
     * The largest limit of the head of a response, in bytes: 1 MiB.
     *
     * The head is held in memory while it is parsed, so this limit bounds the memory a server can make a request
     * take before its body.
     *
     * @since 1.0.0
     */
    public const val MAX_RESPONSE_HEAD_SIZE: Int = 1024 * 1024

    /**
     * The most interim responses, such as `100 Continue`, an HTTP engine reads before the final response: 8.
     *
     * @since 1.0.0
     */
    public const val MAX_INTERIM_RESPONSES: Int = 8

    /**
     * The longest line that announces the size of a chunk of a chunked body, with its extensions, in bytes: 1 KiB.
     *
     * @since 1.0.0
     */
    public const val MAX_CHUNK_LINE_LENGTH: Int = 1024

    /**
     * The longest protocol an upgrade may name, such as `tcp` or `websocket`, in characters: 64.
     *
     * @since 1.0.0
     */
    public const val MAX_UPGRADE_PROTOCOL_LENGTH: Int = 64
}
