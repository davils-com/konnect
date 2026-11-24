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

#include "konnect/native/windows/SecurityAttributes.h"

#include <sddl.h>

namespace konnect::native::windows {

SecurityAttributes::SecurityAttributes(const std::wstring& sddl) noexcept
    : converted(ConvertStringSecurityDescriptorToSecurityDescriptorW(sddl.c_str(), SDDL_REVISION_1,
                                                                     descriptor.receive(), nullptr) != FALSE) {
    attributes.nLength = sizeof(SECURITY_ATTRIBUTES);
    attributes.lpSecurityDescriptor = descriptor.get();
    attributes.bInheritHandle = FALSE;
}

bool SecurityAttributes::isValid() const noexcept {
    return converted;
}

SECURITY_ATTRIBUTES* SecurityAttributes::get() noexcept {
    return &attributes;
}

}
