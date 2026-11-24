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

package com.davils.konnect.ipc.core

/**
 * The bounds Konnect enforces on every name, path, buffer and frame of a local connection between processes.
 *
 * Every entry point of this module checks its input against these limits before it does any work that grows with
 * the size of that input, so a hostile name, path or frame header can neither exhaust the memory of a process nor
 * keep it busy. A value above a limit is refused like any other malformed value, with an exception, a violation or a
 * `null` result as the entry point documents; it is never truncated.
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one is a breaking change.
 * Each limit is a compile-time constant, so code that reads one keeps the value it was compiled against until it is
 * compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The longest name of a named address, in characters.
     *
     * A longer text is refused before the grammar of a name is applied to it.
     *
     * @since 1.0.0
     */
    public const val MAX_NAME_LENGTH: Int = 64

    /**
     * The longest name of a Windows pipe, in UTF-16 code units, without the `\\.\pipe\` prefix.
     *
     * Windows accepts pipe paths of up to 256 characters; the prefix takes 9 of them, and the rest leaves room for
     * the namespace of a named address.
     *
     * @since 1.0.0
     */
    public const val MAX_PIPE_NAME_LENGTH: Int = 240

    /**
     * The longest path of a Unix domain socket, in bytes of its UTF-8 form.
     *
     * The address of a Unix domain socket holds 104 bytes on macOS and 108 bytes on Linux, including the terminating
     * zero; 103 bytes fit both.
     *
     * @since 1.0.0
     */
    public const val MAX_SOCKET_PATH_BYTES: Int = 103

    /**
     * The longest name of a Unix user, in characters.
     *
     * @since 1.0.0
     */
    public const val MAX_USER_NAME_LENGTH: Int = 256

    /**
     * The longest security identifier of a Windows user in its text form, such as `S-1-5-18`, in characters.
     *
     * It covers the longest identifier Windows produces, with an authority of 48 bits and 15 sub-authorities of 32
     * bits each, and is the size of the buffer the identifier of a process is read into.
     *
     * @since 1.0.0
     */
    public const val MAX_SID_LENGTH: Int = 192

    /**
     * The smallest buffer of a connection, in bytes: 1 KiB.
     *
     * @since 1.0.0
     */
    public const val MIN_BUFFER_SIZE: Int = 1024

    /**
     * The largest buffer of a connection, in bytes: 16 MiB.
     *
     * A connection allocates its buffers up front, so this limit bounds the memory every connection takes.
     *
     * @since 1.0.0
     */
    public const val MAX_BUFFER_SIZE: Int = 16 * 1024 * 1024

    /**
     * The buffer of a connection unless a size is given, in bytes: 64 KiB.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_BUFFER_SIZE: Int = 64 * 1024

    /**
     * The largest frame a length prefixed framing receives unless a size is given, in bytes: 16 MiB.
     *
     * The size a frame announces is checked against the limit of its framing before any byte of its body is read
     * or any memory for it is taken.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_MAX_FRAME_SIZE: Int = 16 * 1024 * 1024
}
