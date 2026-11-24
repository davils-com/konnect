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

#pragma once

#include "konnect/native/DirectBuffer.h"

#include <jni.h>
#include <windows.h>

namespace konnect::ipc::core::transport::windows {

class NamedPipe final {
public:
    explicit NamedPipe(jlong handle) noexcept;

    [[nodiscard]] jlong awaitClient() const noexcept;
    [[nodiscard]] jlong read(const native::DirectBuffer& target, jint length) const noexcept;
    [[nodiscard]] jlong write(const native::DirectBuffer& source, jint length) const noexcept;
    [[nodiscard]] jlong cancel() const noexcept;
    [[nodiscard]] jlong close() const noexcept;
    [[nodiscard]] jlong clientProcessId() const noexcept;
    [[nodiscard]] jlong serverProcessId() const noexcept;

private:
    HANDLE handle;
};

}
