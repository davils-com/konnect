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

#include "konnect/ipc/core/transport/windows/PipeEndpoint.h"

#include "konnect/native/NativeResult.h"
#include "konnect/native/windows/Handle.h"
#include "konnect/native/windows/SecurityAttributes.h"
#include "konnect/native/windows/Win32Result.h"

#include <utility>
#include <windows.h>

namespace konnect::ipc::core::transport::windows {

using native::NativeResult;
using native::windows::Handle;
using native::windows::SecurityAttributes;
using native::windows::Win32Result;

PipeEndpoint::PipeEndpoint(std::wstring path) noexcept : path(std::move(path)) {}

jlong PipeEndpoint::create(const std::wstring& sddl, jint bufferSize, bool isFirstInstance) const noexcept {
    if (bufferSize <= 0) {
        return NativeResult::error(ERROR_INVALID_PARAMETER);
    }
    SecurityAttributes security(sddl);
    if (!security.isValid()) {
        return Win32Result::lastError();
    }
    DWORD const firstInstance = isFirstInstance ? FILE_FLAG_FIRST_PIPE_INSTANCE : 0;
    DWORD const openMode = PIPE_ACCESS_DUPLEX | FILE_FLAG_OVERLAPPED | firstInstance;
    DWORD const pipeMode = PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT | PIPE_REJECT_REMOTE_CLIENTS;
    auto const size = static_cast<DWORD>(bufferSize);
    Handle pipe(
        CreateNamedPipeW(path.c_str(), openMode, pipeMode, PIPE_UNLIMITED_INSTANCES, size, size, 0, security.get()));
    if (!pipe.isValid()) {
        return Win32Result::lastError();
    }
    return Win32Result::ofHandle(pipe.release());
}

jlong PipeEndpoint::open() const noexcept {
    DWORD const flags = FILE_FLAG_OVERLAPPED | SECURITY_SQOS_PRESENT | SECURITY_IDENTIFICATION;
    Handle pipe(CreateFileW(path.c_str(), GENERIC_READ | GENERIC_WRITE, 0, nullptr, OPEN_EXISTING, flags, nullptr));
    if (!pipe.isValid()) {
        return Win32Result::lastError();
    }
    return Win32Result::ofHandle(pipe.release());
}

jlong PipeEndpoint::waitAvailable(jint timeoutMillis) const noexcept {
    if (timeoutMillis < 0) {
        return NativeResult::error(ERROR_INVALID_PARAMETER);
    }
    if (WaitNamedPipeW(path.c_str(), static_cast<DWORD>(timeoutMillis)) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(0);
}

}
