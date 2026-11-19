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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

internal class Listener(val port: String, val ports: String) : Validatable {
    var readPort: Port? = null
    var readPorts: PortRange? = null

    override fun ValidationScope.validate() {
        readPort = port(::port, port)
        readPorts = portRange(::ports, ports)
    }
}
