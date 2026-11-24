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

#include "konnect/native/JavaString.h"

#include <cstddef>
#include <span>

namespace konnect::native {

JavaString::JavaString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return;
    }
    const jchar* const characters = env->GetStringChars(value, nullptr);
    if (characters == nullptr) {
        return;
    }
    jsize const length = env->GetStringLength(value);
    std::span<const jchar> const view(characters, static_cast<std::size_t>(length < 0 ? 0 : length));
    try {
        text.assign(view.begin(), view.end());
    } catch (...) {
        env->ReleaseStringChars(value, characters);
        throw;
    }
    env->ReleaseStringChars(value, characters);
    read = true;
}

bool JavaString::isValid() const noexcept {
    return read;
}

const std::u16string& JavaString::get() const noexcept {
    return text;
}

}
