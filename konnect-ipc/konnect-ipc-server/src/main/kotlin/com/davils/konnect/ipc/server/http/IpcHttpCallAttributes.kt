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

import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.address.IpcAddress
import io.ktor.util.AttributeKey

/**
 * The attributes the engine puts on every call, read with `call.attributes[IpcHttpCallAttributes.peer]`.
 *
 * @since 1.0.0
 */
public object IpcHttpCallAttributes {
    /**
     * The process on the other side of the connection, as the operating system reported it and the IPC server checked
     * it; its name is `konnect.ipc.peer`.
     *
     * @since 1.0.0
     */
    public val peer: AttributeKey<IpcPeer> = AttributeKey("konnect.ipc.peer")

    /**
     * The address the call came in on; its name is `konnect.ipc.address`.
     *
     * @since 1.0.0
     */
    public val address: AttributeKey<IpcAddress> = AttributeKey("konnect.ipc.address")
}
