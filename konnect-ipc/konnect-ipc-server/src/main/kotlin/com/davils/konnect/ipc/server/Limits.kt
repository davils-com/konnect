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

package com.davils.konnect.ipc.server

/**
 * The bounds the servers of Konnect enforce on their connections and on the HTTP they serve.
 *
 * A configuration is checked against these limits before a server or an engine is created, and a value outside of
 * them is refused with a violation; it is never clamped. The limits of names, paths and buffers that every local
 * connection shares live in [com.davils.konnect.ipc.core.Limits].
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one is a breaking change.
 * Each limit is a compile-time constant, so code that reads one keeps the value it was compiled against until it is
 * compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The connections a server keeps open at the same time unless another number is given: 64.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_CONNECTIONS: Int = 64

    /**
     * The most connections a server may keep open at the same time: 4,096.
     *
     * Every connection takes its buffers up front, so this ceiling bounds the memory a server can be made to take.
     *
     * @since 1.0.0
     */
    public const val MAX_CONNECTIONS: Int = 4_096

    /**
     * The largest head of a request the HTTP engine reads unless another size is given, in bytes: 32 KiB.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_HEADER_SIZE: Int = 32 * 1024

    /**
     * The largest head of a request the HTTP engine may be told to read, in bytes: 1 MiB.
     *
     * @since 1.0.0
     */
    public const val MAX_HEADER_SIZE: Int = 1024 * 1024

    /**
     * The largest body of a request the HTTP engine takes unless another size is given, in bytes: 64 MiB.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_REQUEST_BODY_SIZE: Long = 64L * 1024 * 1024

    /**
     * The largest body of a request the HTTP engine may be told to take, in bytes: 1 GiB.
     *
     * @since 1.0.0
     */
    public const val MAX_REQUEST_BODY_SIZE: Long = 1024L * 1024 * 1024

    /**
     * The requests the HTTP engine serves on one connection, pipelined or one after the other, unless another number
     * is given: 1,000.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_REQUESTS_PER_CONNECTION: Int = 1_000

    /**
     * The most requests the HTTP engine may be told to serve on one connection: 1,000,000.
     *
     * @since 1.0.0
     */
    public const val MAX_REQUESTS_PER_CONNECTION: Int = 1_000_000

    /**
     * How long the HTTP engine waits for the next request on an idle connection unless another time is given, in
     * milliseconds: 45 seconds.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_IDLE_TIMEOUT_MILLIS: Long = 45_000L

    /**
     * How long the whole body of a request may take to arrive unless another time is given, in milliseconds: 30
     * seconds.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_REQUEST_BODY_TIMEOUT_MILLIS: Long = 30_000L

    /**
     * The longest idle timeout and request body timeout the HTTP engine may be told to wait, in milliseconds: one
     * hour.
     *
     * @since 1.0.0
     */
    public const val MAX_TIMEOUT_MILLIS: Long = 3_600_000L
}
