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

package com.davils.konnect.ipc.core.address

/**
 * Who a named address belongs to, which decides where its socket or pipe lives.
 *
 * @since 1.0.0
 */
public enum class IpcScope {
    /**
     * The address belongs to the current user: its socket lives in a directory of that user and its pipe carries the
     * security identifier of that user in its name.
     *
     * @since 1.0.0
     */
    User,

    /**
     * The address belongs to the system: its socket lives in the runtime directory of the system and its pipe in the
     * namespace of Konnect, so that every user can reach it.
     *
     * @since 1.0.0
     */
    System,
}
