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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations that the validations of local connections between processes report.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object IpcViolationCodes {
    /**
     * The code of an access that lets nobody in, because it neither allows the current user nor names anybody; its
     * value is `konnect.ipc.nobody-allowed`.
     *
     * @since 1.0.0
     */
    public val NOBODY_ALLOWED: ViolationCode = ViolationCode("konnect.ipc.nobody-allowed")

    /**
     * The code of a buffer size outside of [Limits.MIN_BUFFER_SIZE] and [Limits.MAX_BUFFER_SIZE]; its value is
     * `konnect.ipc.invalid-buffer-size`.
     *
     * @since 1.0.0
     */
    public val INVALID_BUFFER_SIZE: ViolationCode = ViolationCode("konnect.ipc.invalid-buffer-size")
}
