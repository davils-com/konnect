#
# Copyright 2026 Davils
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include_guard(GLOBAL)

function(konnect_harden target)
    konnect_require_msvc()
    set_target_properties(${target} PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        MSVC_RUNTIME_LIBRARY "MultiThreaded"
        POSITION_INDEPENDENT_CODE ON
    )
    target_compile_options(${target} PRIVATE /W4 /WX /permissive- /sdl /guard:cf /Qspectre /utf-8 /EHsc)
    target_compile_definitions(${target} PRIVATE UNICODE _UNICODE WIN32_LEAN_AND_MEAN NOMINMAX)
    konnect_harden_link_options(${target})
    konnect_harden_architecture(${target})
    konnect_clang_tidy(${target})
endfunction()

function(konnect_require_msvc)
    if(NOT CMAKE_CXX_COMPILER_ID STREQUAL "MSVC")
        message(FATAL_ERROR "The native library of konnect is built with MSVC only, so that control flow guard and Spectre mitigation apply.")
    endif()
endfunction()

function(konnect_harden_link_options target)
    get_target_property(type ${target} TYPE)
    if(NOT type STREQUAL "SHARED_LIBRARY")
        return()
    endif()
    target_link_options(${target} PRIVATE /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /GUARD:CF)
endfunction()

function(konnect_harden_architecture target)
    if(CMAKE_CXX_COMPILER_ARCHITECTURE_ID STREQUAL "ARM64")
        target_compile_options(${target} PRIVATE /guard:signret)
        return()
    endif()
    get_target_property(type ${target} TYPE)
    if(type STREQUAL "SHARED_LIBRARY")
        target_link_options(${target} PRIVATE /CETCOMPAT)
    endif()
endfunction()

function(konnect_clang_tidy target)
    set(konnect_llvm_architecture x64)
    if(CMAKE_HOST_SYSTEM_PROCESSOR MATCHES "^(ARM64|arm64|aarch64)$")
        set(konnect_llvm_architecture ARM64)
    endif()
    find_program(KONNECT_CLANG_TIDY clang-tidy
        HINTS "$ENV{VCINSTALLDIR}Tools/Llvm/${konnect_llvm_architecture}/bin"
        REQUIRED
    )
    set_target_properties(${target} PROPERTIES CXX_CLANG_TIDY "${KONNECT_CLANG_TIDY};--extra-arg=/EHsc")
endfunction()
