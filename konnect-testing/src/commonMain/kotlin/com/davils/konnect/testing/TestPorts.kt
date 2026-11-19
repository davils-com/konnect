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

package com.davils.konnect.testing

import com.davils.konnect.core.port.Port
import com.davils.konnect.core.port.PortCategory
import com.davils.konnect.core.port.PortRange

private const val DYNAMIC_START: Int = 49_152
private const val DYNAMIC_COUNT: Int = 16_384

/**
 * Ports for tests: fixed ports of the dynamic range of RFC 6335, which IANA never assigns to a service.
 *
 * The same arguments always give the same ports, so a test that names them is repeatable. A port of this object is
 * only a value: nothing here checks whether it is free on the machine that runs the test.
 *
 * @since 1.0.0
 */
public object TestPorts {
    /**
     * Returns the dynamic port with the number `49152 + index`.
     *
     * @param index the position of the port in the dynamic range, from `0` to `16383`.
     * @return a port of the category [PortCategory.Dynamic].
     * @throws IllegalArgumentException if [index] is outside `0..16383`.
     * @since 1.0.0
     */
    public fun dynamic(index: Int = 0): Port {
        require(index in 0 until DYNAMIC_COUNT) {
            "There are $DYNAMIC_COUNT dynamic ports, but index $index was asked for"
        }
        return Port(DYNAMIC_START + index)
    }

    /**
     * Returns the range of [size] dynamic ports that starts at `49152`.
     *
     * @param size the number of ports in the range, from `1` to `16384`.
     * @return a range of ports of the category [PortCategory.Dynamic].
     * @throws IllegalArgumentException if [size] is outside `1..16384`.
     * @since 1.0.0
     */
    public fun dynamicRange(size: Int = 1): PortRange {
        require(size in 1..DYNAMIC_COUNT) { "A range of dynamic ports holds 1 to $DYNAMIC_COUNT ports, but was $size" }
        return PortRange(first = dynamic(), last = dynamic(size - 1))
    }
}
