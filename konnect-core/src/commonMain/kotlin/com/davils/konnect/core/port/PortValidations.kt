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

import com.davils.konnect.core.port.grammar.PortRangeReading
import com.davils.konnect.core.port.grammar.PortReading
import com.davils.konnect.core.port.grammar.readPort
import com.davils.konnect.core.port.grammar.readPortRange
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

/**
 * Reads [text] as a port inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [Port.parse] reads it, with the same limits. A text that is not a port is reported as an
 * error with the code [PortViolationCodes.INVALID_PORT] and a message such as `is not a port: it has a leading
 * zero`. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the port, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.port(property: KProperty<*>, text: String): Port? = when (val reading = readPort(text)) {
    is PortReading.Read -> Port(reading.value)

    is PortReading.Refused -> refused(
        property = property,
        message = "is not a port: it ${reading.reason}",
        code = PortViolationCodes.INVALID_PORT,
    )
}

/**
 * Reads [text] as a port range inside a validation, reporting a violation at [property] instead of throwing.
 *
 * The text is read as [PortRange.parse] reads it, with the same limits. A text that is not a port range is reported
 * as an error with the code [PortViolationCodes.INVALID_PORT_RANGE] and a message such as `is not a port range: it
 * starts after it ends`. The message names the problem in fixed words and never quotes [text].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the text belongs to; the violation is reported at it.
 * @param text the text to read; it may come from an attacker.
 * @return the range, or `null` if [text] is not one and a violation was reported.
 * @since 1.0.0
 */
public fun ValidationScope.portRange(property: KProperty<*>, text: String): PortRange? =
    when (val reading = readPortRange(text)) {
        is PortRangeReading.Read -> PortRange.fromReading(reading)

        is PortRangeReading.Refused -> refused(
            property = property,
            message = "is not a port range: it ${reading.reason}",
            code = PortViolationCodes.INVALID_PORT_RANGE,
        )
    }

private fun ValidationScope.refused(property: KProperty<*>, message: String, code: ViolationCode): Nothing? {
    fail(property = property, message = message, code = code)
    return null
}
