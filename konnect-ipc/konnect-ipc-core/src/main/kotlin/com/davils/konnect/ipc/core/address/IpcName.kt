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

package com.davils.konnect.ipc.core.address

import com.davils.konnect.ipc.core.Limits

private val NAME_PATTERN: Regex = Regex("[a-z0-9][a-z0-9._-]*")

/**
 * The name of a named address, such as `helper` or `com.example.agent-1`.
 *
 * A name starts with a lower case ASCII letter or digit, continues with lower case ASCII letters, digits, dots,
 * dashes and underscores, and holds at most [Limits.MAX_NAME_LENGTH] characters. It names a socket file or a pipe and
 * can therefore hold neither a separator nor a character a file system would read in another way. Instances are
 * immutable.
 *
 * @property value the name.
 * @throws IllegalArgumentException if [value] is not a name; the message never quotes it.
 * @since 1.0.0
 */
@JvmInline
public value class IpcName(public val value: String) {
    init {
        require(isValid(value)) { "Not an ipc name: it must be lower case letters, digits, dots, dashes, underscores" }
    }

    /**
     * Shows the name as it is.
     *
     * @return the name.
     * @since 1.0.0
     */
    override fun toString(): String = value

    /**
     * Checks and reads names.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Tells whether [text] is a name.
         *
         * A text longer than [Limits.MAX_NAME_LENGTH] is refused before the grammar is applied to it.
         *
         * @param text the text to check; it may come from an attacker.
         * @return `true` if [text] is a name.
         * @since 1.0.0
         */
        public fun isValid(text: String): Boolean {
            if (text.length !in 1..Limits.MAX_NAME_LENGTH) return false
            return NAME_PATTERN.matches(text)
        }

        /**
         * Reads [text] as a name, as the constructor does, but returns `null` instead of throwing.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the name, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IpcName? {
            if (!isValid(text)) return null
            return IpcName(text)
        }
    }
}
