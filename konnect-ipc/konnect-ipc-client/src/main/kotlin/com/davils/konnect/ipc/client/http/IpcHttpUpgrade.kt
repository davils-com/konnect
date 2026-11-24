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

package com.davils.konnect.ipc.client.http

import com.davils.konnect.ipc.client.Limits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.bytestring.ByteString

private const val HASH_FACTOR: Int = 31
private const val TOKEN_SYMBOLS: String = "!#$%&'*+-.^_`|~/"
private val tokenCharacters: Set<Char> = buildSet {
    addAll('a'..'z')
    addAll('A'..'Z')
    addAll('0'..'9')
    addAll(TOKEN_SYMBOLS.asIterable())
}

/**
 * The protocol a request switches its connection to, and what it sends once the server switched; see
 * [IpcHttpUpgradeCapability].
 *
 * @property protocol the protocol, such as `tcp`: from 1 to [Limits.MAX_UPGRADE_PROTOCOL_LENGTH] letters, digits and
 *   the symbols of an HTTP token, with an optional `/` before its version.
 * @property input what is written to the server after the switch; nothing unless it is given.
 * @throws IllegalArgumentException if [protocol] is not a token; the message never quotes it.
 * @since 1.0.0
 */
public class IpcHttpUpgrade(public val protocol: String, public val input: Flow<ByteString> = emptyFlow()) {
    init {
        require(protocol.length in 1..Limits.MAX_UPGRADE_PROTOCOL_LENGTH && protocol.all(tokenCharacters::contains)) {
            "An upgrade must name an HTTP token of at most ${Limits.MAX_UPGRADE_PROTOCOL_LENGTH} characters"
        }
    }

    /**
     * Tells whether [other] switches to the same protocol with the same input.
     *
     * @param other the value to compare with.
     * @return `true` if [other] is an equal upgrade.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is IpcHttpUpgrade && protocol == other.protocol && input == other.input

    /**
     * Computes a hash code from the protocol and the input.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * protocol.hashCode() + input.hashCode()

    /**
     * Shows the protocol, but not the input.
     *
     * @return the upgrade as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IpcHttpUpgrade($protocol)"
}
