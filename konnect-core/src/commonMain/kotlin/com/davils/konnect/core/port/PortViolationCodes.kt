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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations that the validations of ports and port ranges report.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object PortViolationCodes {
    /**
     * The code of a text that is not a port, reported by [port]; its value is `konnect.port.invalid-port`.
     *
     * @since 1.0.0
     */
    public val INVALID_PORT: ViolationCode = ViolationCode("konnect.port.invalid-port")

    /**
     * The code of a text that is not a port range and of a range whose last port is below its first, reported by
     * [portRange] and by the validation of [PortRange]; its value is `konnect.port.invalid-port-range`.
     *
     * @since 1.0.0
     */
    public val INVALID_PORT_RANGE: ViolationCode = ViolationCode("konnect.port.invalid-port-range")
}
