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
#include "konnect/ipc/core/transport/windows/PipeEndpoint.h"
#include "konnect/ipc/core/transport/windows/ProcessIdentity.h"
#include "konnect/native/DirectBuffer.h"
#include "konnect/native/JavaString.h"
#include "konnect/native/NativeResult.h"
#include "konnect/native/windows/Guarded.h"
#include "konnect/native/windows/WideString.h"
#include "konnect_ipc_core_jni.h"

using konnect::ipc::core::transport::windows::NamedPipe;
using konnect::ipc::core::transport::windows::PipeEndpoint;
using konnect::ipc::core::transport::windows::ProcessIdentity;
using konnect::native::DirectBuffer;
using konnect::native::JavaString;
using konnect::native::NativeResult;
using konnect::native::windows::guarded;
using konnect::native::windows::WideString;

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_createServer(
    JNIEnv* env, [[maybe_unused]] jclass type, jstring path, jstring securityDescriptor, jint bufferSize,
    jboolean isFirstInstance) {
    return guarded([&]() -> jlong {
        WideString const pipe(JavaString(env, path));
        WideString const sddl(JavaString(env, securityDescriptor));
        if (!pipe.isValid() || !sddl.isValid()) {
            return NativeResult::error(ERROR_INVALID_PARAMETER);
        }
        return PipeEndpoint(pipe.get()).create(sddl.get(), bufferSize, isFirstInstance == JNI_TRUE);
    });
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_awaitClient(
    [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass type, jlong handle) {
    return NamedPipe(handle).awaitClient();
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_open(
    JNIEnv* env, [[maybe_unused]] jclass type, jstring path) {
    return guarded([&]() -> jlong {
        WideString const pipe(JavaString(env, path));
        if (!pipe.isValid()) {
            return NativeResult::error(ERROR_INVALID_PARAMETER);
        }
        return PipeEndpoint(pipe.get()).open();
    });
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_waitAvailable(
    JNIEnv* env, [[maybe_unused]] jclass type, jstring path, jint timeoutMillis) {
    return guarded([&]() -> jlong {
        WideString const pipe(JavaString(env, path));
        if (!pipe.isValid()) {
            return NativeResult::error(ERROR_INVALID_PARAMETER);
        }
        return PipeEndpoint(pipe.get()).waitAvailable(timeoutMillis);
    });
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_read(
    JNIEnv* env, [[maybe_unused]] jclass type, jlong handle, jobject buffer, jint length) {
    return NamedPipe(handle).read(DirectBuffer(env, buffer), length);
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_write(
    JNIEnv* env, [[maybe_unused]] jclass type, jlong handle, jobject buffer, jint length) {
    return NamedPipe(handle).write(DirectBuffer(env, buffer), length);
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_cancel(
    [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass type, jlong handle) {
    return NamedPipe(handle).cancel();
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_close(
    [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass type, jlong handle) {
    return NamedPipe(handle).close();
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_clientProcessId(
    [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass type, jlong handle) {
    return NamedPipe(handle).clientProcessId();
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_serverProcessId(
    [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass type, jlong handle) {
    return NamedPipe(handle).serverProcessId();
}

JNIEXPORT jlong JNICALL Java_com_davils_konnect_ipc_core_transport_windows_NamedPipeNative_processUserSid(
    JNIEnv* env, [[maybe_unused]] jclass type, jlong processId, jobject buffer) {
    return guarded([&]() -> jlong { return ProcessIdentity(processId).writeUserSid(DirectBuffer(env, buffer)); });
}
