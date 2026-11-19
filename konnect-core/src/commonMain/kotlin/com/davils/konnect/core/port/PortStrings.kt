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

package com.davils.konnect.core.port

/**
 * Reads this text as a port, as [Port.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the port.
 * @throws IllegalArgumentException if this text is not a port; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toPort(): Port = Port.parse(this)

/**
 * Reads this text as a port, as [Port.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the port, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toPortOrNull(): Port? = Port.parseOrNull(this)

/**
 * Reads this text as a port range, as [PortRange.parse] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the range.
 * @throws IllegalArgumentException if this text is not a port range; the message never quotes the text.
 * @since 1.0.0
 */
public fun String.toPortRange(): PortRange = PortRange.parse(this)

/**
 * Reads this text as a port range, as [PortRange.parseOrNull] does.
 *
 * @receiver the text to read; it may come from an attacker.
 * @return the range, or `null` if this text is not one.
 * @since 1.0.0
 */
public fun String.toPortRangeOrNull(): PortRange? = PortRange.parseOrNull(this)
