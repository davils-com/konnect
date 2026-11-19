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

package com.davils.konnect.internetprotocol.subnet

import com.davils.konnect.internetprotocol.IpVersion
import com.davils.konnect.internetprotocol.IpViolationCodes
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

internal fun ValidationScope.checkPrefixLength(
    property: KProperty<*>,
    prefixLength: Int,
    version: IpVersion,
): Boolean {
    val isValid = prefixLength in 0..version.bitLength
    if (!isValid) {
        fail(
            property = property,
            message = "must be in 0..${version.bitLength}, but was $prefixLength",
            code = IpViolationCodes.INVALID_PREFIX,
        )
    }
    return isValid
}
