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

import com.davils.kore.core.arguments.conversion.ConversionException
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters

/**
 * Converters of Kore for ports and port ranges, so that a command line parameter of Kore, an environment read of Knot
 * or a property of Kontext reads them typed, such as `property("server.port", PortConverters.port)`.
 *
 * Every converter reads exactly the text [Port.parse] and [PortRange.parse] read and refuses everything else with a
 * [ConversionException] of constant text that never repeats the input.
 *
 * @since 1.1.0
 */
public object PortConverters {
    /**
     * Reads a [Port], such as `8080`; its type name is `port`.
     *
     * @since 1.1.0
     */
    public val port: Converter<Port> = Converters.custom(typeName = "port") { raw ->
        Port.parseOrNull(raw) ?: throw ConversionException("is no port from 0 to 65535")
    }

    /**
     * Reads a [PortRange], such as `8000-8080`; its type name is `port range`.
     *
     * @since 1.1.0
     */
    public val portRange: Converter<PortRange> = Converters.custom(typeName = "port range") { raw ->
        PortRange.parseOrNull(raw) ?: throw ConversionException("is no port range")
    }
}
