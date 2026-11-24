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

#include "konnect/native/windows/OverlappedOperation.h"

#include "konnect/native/NativeResult.h"
#include "konnect/native/windows/Win32Result.h"

namespace konnect::native::windows {

OverlappedOperation::OverlappedOperation() noexcept : event(CreateEventW(nullptr, TRUE, FALSE, nullptr)) {
    overlapped.hEvent = event.get();
}

bool OverlappedOperation::isReady() const noexcept {
    return event.isValid();
}

OVERLAPPED* OverlappedOperation::get() noexcept {
    return &overlapped;
}

jlong OverlappedOperation::complete(HANDLE file, BOOL isStarted) noexcept {
    DWORD const started = GetLastError();
    if (isStarted == FALSE && started != ERROR_IO_PENDING) {
        return NativeResult::error(started);
    }
    DWORD transferred = 0;
    if (GetOverlappedResult(file, &overlapped, &transferred, TRUE) == FALSE) {
        return Win32Result::lastError();
    }
    return NativeResult::of(transferred);
}

}
