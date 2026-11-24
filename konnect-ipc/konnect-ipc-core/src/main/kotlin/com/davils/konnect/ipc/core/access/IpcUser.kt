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

package com.davils.konnect.ipc.core.access

import com.davils.konnect.ipc.core.Limits

private val SID_PATTERN: Regex = Regex("S-1(-\\d+){1,15}")

/**
 * The user a process runs as, as the operating system names it.
 *
 * Every user is immutable and checked when it is created.
 *
 * @since 1.0.0
 */
public sealed interface IpcUser {
    /**
     * A Windows user, named by the text form of its security identifier, such as `S-1-5-18`.
     *
     * The identifier is `S-1` followed by 1 to 15 dash-separated decimal numbers and holds at most
     * [Limits.MAX_SID_LENGTH] characters.
     *
     * @property value the security identifier.
     * @throws IllegalArgumentException if [value] is not a security identifier; the message never quotes it.
     * @since 1.0.0
     */
    public class WindowsSid(public val value: String) : IpcUser {
        init {
            require(isValid(value)) { "Not a security identifier: it must be S-1 followed by up to 15 numbers" }
        }

        /**
         * Tells whether [other] is the same security identifier.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal identifier.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is WindowsSid && value == other.value

        /**
         * Computes a hash code from the identifier.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Shows the identifier as it is.
         *
         * @return the identifier.
         * @since 1.0.0
         */
        override fun toString(): String = value

        /**
         * Well-known identifiers and the check of identifiers.
         *
         * @since 1.0.0
         */
        public companion object {
            /**
             * The local system account, `S-1-5-18`.
             *
             * @since 1.0.0
             */
            public val LOCAL_SYSTEM: WindowsSid = WindowsSid("S-1-5-18")

            /**
             * Tells whether [text] is a security identifier.
             *
             * A text longer than [Limits.MAX_SID_LENGTH] is refused before the grammar is applied to it.
             *
             * @param text the text to check; it may come from an attacker.
             * @return `true` if [text] is a security identifier.
             * @since 1.0.0
             */
            public fun isValid(text: String): Boolean {
                if (text.length > Limits.MAX_SID_LENGTH) return false
                return SID_PATTERN.matches(text)
            }
        }
    }

    /**
     * A Unix user, named by its user name, such as `root`.
     *
     * The name holds from 1 to [Limits.MAX_USER_NAME_LENGTH] characters, is not blank and holds no control
     * character.
     *
     * @property name the user name.
     * @throws IllegalArgumentException if [name] is not a user name; the message never quotes it.
     * @since 1.0.0
     */
    public class UnixUser(public val name: String) : IpcUser {
        init {
            require(isValid(name)) { "Not a user name: it must not be blank or hold control characters" }
        }

        /**
         * Tells whether [other] is the user of the same name.
         *
         * @param other the value to compare with.
         * @return `true` if [other] is an equal user.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is UnixUser && name == other.name

        /**
         * Computes a hash code from the name.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = name.hashCode()

        /**
         * Shows the name as it is.
         *
         * @return the user name.
         * @since 1.0.0
         */
        override fun toString(): String = name

        /**
         * Well-known users and the check of user names.
         *
         * @since 1.0.0
         */
        public companion object {
            /**
             * The superuser, `root`.
             *
             * @since 1.0.0
             */
            public val ROOT: UnixUser = UnixUser("root")

            /**
             * Tells whether [text] is a user name.
             *
             * A text longer than [Limits.MAX_USER_NAME_LENGTH] is refused before its characters are looked at.
             *
             * @param text the text to check; it may come from an attacker.
             * @return `true` if [text] is a user name.
             * @since 1.0.0
             */
            public fun isValid(text: String): Boolean {
                if (text.length > Limits.MAX_USER_NAME_LENGTH || text.isBlank()) return false
                return text.none(Char::isISOControl)
            }
        }
    }
}
