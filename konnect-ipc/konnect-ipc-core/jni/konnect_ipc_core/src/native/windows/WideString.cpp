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

#include "konnect/native/windows/WideString.h"

namespace konnect::native::windows {

static_assert(sizeof(wchar_t) == sizeof(char16_t));

WideString::WideString(const JavaString& value)
    : text(value.get().begin(), value.get().end()),
      valid(value.isValid() && !text.empty() && text.find(L'\0') == std::wstring::npos) {}

bool WideString::isValid() const noexcept {
    return valid;
}

const std::wstring& WideString::get() const noexcept {
    return text;
}

}
