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

#include "konnect/ipc/core/transport/windows/NamedPipe.h"

#include "konnect/native/NativeResult.h"
#include "konnect/native/windows/OverlappedOperation.h"
#include "konnect/native/windows/Win32Result.h"

namespace konnect::ipc::core::transport::windows {

using native::DirectBuffer;
using native::NativeResult;
using native::windows::OverlappedOperation;
using native::windows::Win32Result;

NamedPipe::NamedPipe(jlong handle) noexcept : handle(Win32Result::handleOf(handle)) {}

jlong NamedPipe::awaitClient() const noexcept {
    OverlappedOperation operation;
    if (!operation.isReady()) {
        return Win32Result::lastError();
    }
    return operation.complete(handle, ConnectNamedPipe(handle, operation.get()));
}

jlong NamedPipe::read(const DirectBuffer& target, jint length) const noexcept {
    if (!target.fits(length)) {
        return NativeResult::error(ERROR_INVALID_PARAMETER);
    }
    OverlappedOperation operation;
    if (!operation.isReady()) {
        return Win32Result::lastError();
    }
    return operation.complete(handle,
                              ReadFile(handle, target.data(), static_cast<DWORD>(length), nullptr, operation.get()));
}

jlong NamedPipe::write(const DirectBuffer& source, jint length) const noexcept {
    if (!source.fits(length)) {
        return NativeResult::error(ERROR_INVALID_PARAMETER);
    }
    OverlappedOperation operation;
    if (!operation.isReady()) {
        return Win32Result::lastError();
    }
    return operation.complete(handle,
                              WriteFile(handle, source.data(), static_cast<DWORD>(length), nullptr, operation.get()));
}

jlong NamedPipe::cancel() const noexcept {
    if (CancelIoEx(handle, nullptr) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(0);
}

jlong NamedPipe::close() const noexcept {
    if (CloseHandle(handle) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(0);
}

jlong NamedPipe::clientProcessId() const noexcept {
    ULONG processId = 0;
    if (GetNamedPipeClientProcessId(handle, &processId) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(processId);
}

jlong NamedPipe::serverProcessId() const noexcept {
    ULONG processId = 0;
    if (GetNamedPipeServerProcessId(handle, &processId) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(processId);
}

}
