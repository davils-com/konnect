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

package com.davils.konnect.ipc.server.http

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations that the configuration of the HTTP engine reports.
 *
 * The values of the codes are stable and may be stored, compared and matched by consumers.
 *
 * @since 1.0.0
 */
public object IpcHttpServerViolationCodes {
    /**
     * The code of a configuration that names no IPC server; its value is `konnect.ipc.server.http.missing-server`.
     *
     * @since 1.0.0
     */
    public val MISSING_SERVER: ViolationCode = ViolationCode("konnect.ipc.server.http.missing-server")

    /**
     * The code of an idle timeout that is not positive or longer than
     * [com.davils.konnect.ipc.server.Limits.MAX_TIMEOUT_MILLIS]; its value is
     * `konnect.ipc.server.http.invalid-idle-timeout`.
     *
     * @since 1.0.0
     */
    public val INVALID_IDLE_TIMEOUT: ViolationCode = ViolationCode("konnect.ipc.server.http.invalid-idle-timeout")

    /**
     * The code of a request body timeout that is not positive or longer than
     * [com.davils.konnect.ipc.server.Limits.MAX_TIMEOUT_MILLIS]; its value is
     * `konnect.ipc.server.http.invalid-request-body-timeout`.
     *
     * @since 1.0.0
     */
    public val INVALID_REQUEST_BODY_TIMEOUT: ViolationCode =
        ViolationCode("konnect.ipc.server.http.invalid-request-body-timeout")

    /**
     * The code of a head size outside of 1 and [com.davils.konnect.ipc.server.Limits.MAX_HEADER_SIZE]; its value is
     * `konnect.ipc.server.http.invalid-max-header-size`.
     *
     * @since 1.0.0
     */
    public val INVALID_MAX_HEADER_SIZE: ViolationCode = ViolationCode("konnect.ipc.server.http.invalid-max-header-size")

    /**
     * The code of a body size outside of 0 and [com.davils.konnect.ipc.server.Limits.MAX_REQUEST_BODY_SIZE]; its value
     * is `konnect.ipc.server.http.invalid-max-request-body-size`.
     *
     * @since 1.0.0
     */
    public val INVALID_MAX_REQUEST_BODY_SIZE: ViolationCode =
        ViolationCode("konnect.ipc.server.http.invalid-max-request-body-size")

    /**
     * The code of a number of requests per connection outside of 1 and
     * [com.davils.konnect.ipc.server.Limits.MAX_REQUESTS_PER_CONNECTION]; its value is
     * `konnect.ipc.server.http.invalid-max-requests-per-connection`.
     *
     * @since 1.0.0
     */
    public val INVALID_MAX_REQUESTS_PER_CONNECTION: ViolationCode =
        ViolationCode("konnect.ipc.server.http.invalid-max-requests-per-connection")

    /**
     * The code of a configuration that declares connectors, although the IPC server names the address; its value is
     * `konnect.ipc.server.http.unexpected-connector`.
     *
     * @since 1.0.0
     */
    public val UNEXPECTED_CONNECTOR: ViolationCode = ViolationCode("konnect.ipc.server.http.unexpected-connector")
}
