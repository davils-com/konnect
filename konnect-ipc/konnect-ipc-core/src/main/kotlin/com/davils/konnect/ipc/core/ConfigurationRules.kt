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

package com.davils.konnect.ipc.core

import com.davils.konnect.ipc.core.address.IpcAddress
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

/**
 * Reports a missing [address] at [property] with the code [ViolationCode.REQUIRED].
 *
 * @receiver the validation that collects the violation.
 * @param property the property the address belongs to.
 * @param address the address of a configuration, or `null` if none was declared.
 * @since 1.0.0
 */
@DavilsInternal
public fun ValidationScope.checkIpcAddress(property: KProperty<*>, address: IpcAddress?) {
    if (address != null) return
    fail(property = property, message = "is required", code = ViolationCode.REQUIRED)
}

/**
 * Reports a [bufferSize] outside of [Limits.MIN_BUFFER_SIZE] and [Limits.MAX_BUFFER_SIZE] at [property] with the
 * code [IpcViolationCodes.INVALID_BUFFER_SIZE].
 *
 * The message names the limits and never the size it refused.
 *
 * @receiver the validation that collects the violation.
 * @param property the property the size belongs to.
 * @param bufferSize the size of the buffer, in bytes.
 * @since 1.0.0
 */
@DavilsInternal
public fun ValidationScope.checkIpcBufferSize(property: KProperty<*>, bufferSize: Int) {
    if (bufferSize in Limits.MIN_BUFFER_SIZE..Limits.MAX_BUFFER_SIZE) return
    fail(
        property = property,
        message = "must be from ${Limits.MIN_BUFFER_SIZE} to ${Limits.MAX_BUFFER_SIZE} bytes",
        code = IpcViolationCodes.INVALID_BUFFER_SIZE,
    )
}
