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

#include <jni.h>
#include <string>

namespace konnect::ipc::core::transport::windows {

class PipeEndpoint final {
public:
    explicit PipeEndpoint(std::wstring path) noexcept;

    [[nodiscard]] jlong create(const std::wstring& sddl, jint bufferSize, bool isFirstInstance) const noexcept;
    [[nodiscard]] jlong open() const noexcept;
    [[nodiscard]] jlong waitAvailable(jint timeoutMillis) const noexcept;

private:
    std::wstring path;
};

}
