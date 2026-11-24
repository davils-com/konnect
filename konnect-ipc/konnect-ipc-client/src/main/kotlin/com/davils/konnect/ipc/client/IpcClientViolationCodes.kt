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

package com.davils.konnect.ipc.client

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations that the validation of a client configuration reports, next to the codes of
 * [com.davils.konnect.ipc.core.IpcViolationCodes] and of Kore.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object IpcClientViolationCodes {
    /**
     * The code of a connect timeout that is not positive and finite; its value is
     * `konnect.ipc.client.invalid-connect-timeout`.
     *
     * @since 1.0.0
     */
    public val INVALID_CONNECT_TIMEOUT: ViolationCode = ViolationCode("konnect.ipc.client.invalid-connect-timeout")
}
