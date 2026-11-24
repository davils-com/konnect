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
    set_target_properties(${target} PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        MSVC_RUNTIME_LIBRARY "MultiThreaded"
        POSITION_INDEPENDENT_CODE ON
    )
    konnect_harden_compile_options(${target})
    konnect_harden_link_options(${target})
    konnect_harden_windows(${target})
endfunction()

function(konnect_harden_compile_options target)
    if(MSVC)
        target_compile_options(${target} PRIVATE /W4 /WX /permissive- /sdl /guard:cf /Qspectre /utf-8 /EHsc)
        return()
    endif()
    target_compile_options(${target} PRIVATE -Wall -Wextra -Werror)
endfunction()

function(konnect_harden_link_options target)
    get_target_property(type ${target} TYPE)
    if(NOT MSVC OR NOT type STREQUAL "SHARED_LIBRARY")
        return()
    endif()
    target_link_options(${target} PRIVATE /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /CETCOMPAT /GUARD:CF)
endfunction()

function(konnect_harden_windows target)
    if(NOT WIN32)
        return()
    endif()
    set(konnect_llvm_architecture x64)
    if(CMAKE_HOST_SYSTEM_PROCESSOR MATCHES "^(ARM64|arm64|aarch64)$")
        set(konnect_llvm_architecture ARM64)
    endif()
    find_program(KONNECT_CLANG_TIDY clang-tidy
        HINTS "$ENV{VCINSTALLDIR}Tools/Llvm/${konnect_llvm_architecture}/bin"
        REQUIRED
    )
    target_compile_definitions(${target} PRIVATE UNICODE _UNICODE WIN32_LEAN_AND_MEAN NOMINMAX)
    set(konnect_clang_tidy "${KONNECT_CLANG_TIDY}")
    if(MSVC)
        list(APPEND konnect_clang_tidy "--extra-arg-before=--driver-mode=cl")
    endif()
    set_target_properties(${target} PROPERTIES CXX_CLANG_TIDY "${konnect_clang_tidy}")
endfunction()
