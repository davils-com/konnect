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

#include "konnect/ipc/core/transport/windows/ProcessIdentity.h"

#include "konnect/native/NativeResult.h"
#include "konnect/native/windows/Handle.h"
#include "konnect/native/windows/LocalMemory.h"
#include "konnect/native/windows/Win32Result.h"

#include <algorithm>
#include <cstdint>
#include <cwchar>
#include <limits>
#include <sddl.h>
#include <vector>

namespace konnect::ipc::core::transport::windows {

using native::DirectBuffer;
using native::NativeResult;
using native::windows::Handle;
using native::windows::LocalMemory;
using native::windows::Win32Result;

ProcessIdentity::ProcessIdentity(jlong processId) noexcept : processId(processId) {}

jlong ProcessIdentity::writeUserSid(const DirectBuffer& target) const {
    if (processId < 0 || processId > static_cast<jlong>(std::numeric_limits<DWORD>::max())) {
        return NativeResult::error(ERROR_INVALID_PARAMETER);
    }
    Handle const process(OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, static_cast<DWORD>(processId)));
    HANDLE opened = nullptr;
    if (!process.isValid() || OpenProcessToken(process.get(), TOKEN_QUERY, &opened) == FALSE) {
        return Win32Result::lastError();
    }
    Handle const token(opened);
    DWORD size = 0;
    if (GetTokenInformation(token.get(), TokenUser, nullptr, 0, &size) == FALSE && size == 0) {
        return Win32Result::lastError();
    }
    std::vector<BYTE> user(size);
    if (GetTokenInformation(token.get(), TokenUser, user.data(), size, &size) == FALSE) {
        return Win32Result::lastError();
    }
    LocalMemory text;
    auto const* const owner = reinterpret_cast<const TOKEN_USER*>(user.data());
    if (ConvertSidToStringSidW(owner->User.Sid, reinterpret_cast<LPWSTR*>(text.receive())) == FALSE) {
        return Win32Result::lastError();
    }
    auto const* const sid = static_cast<const wchar_t*>(text.get());
    auto const length = static_cast<jlong>(std::wcslen(sid));
    if (!target.fits((length + 1) * static_cast<jlong>(sizeof(wchar_t)))) {
        return NativeResult::error(ERROR_INSUFFICIENT_BUFFER);
    }
    std::copy_n(sid, length, static_cast<wchar_t*>(target.data()));
    return NativeResult::of(static_cast<std::uint32_t>(length));
}

}
