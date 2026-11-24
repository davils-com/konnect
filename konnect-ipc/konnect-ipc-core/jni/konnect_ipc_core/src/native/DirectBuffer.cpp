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

#include "konnect/native/DirectBuffer.h"

namespace konnect::native {

DirectBuffer::DirectBuffer(JNIEnv* env, jobject buffer) noexcept
    : address(buffer == nullptr ? nullptr : env->GetDirectBufferAddress(buffer)),
      capacity(buffer == nullptr ? -1 : env->GetDirectBufferCapacity(buffer)) {}

bool DirectBuffer::fits(jlong length) const noexcept {
    return address != nullptr && capacity >= 0 && length >= 0 && length <= capacity;
}

void* DirectBuffer::data() const noexcept {
    return address;
}

jlong DirectBuffer::size() const noexcept {
    return capacity;
}

}
