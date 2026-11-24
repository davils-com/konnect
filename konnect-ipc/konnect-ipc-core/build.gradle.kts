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

plugins {
    id("konnect.jvm-native-library")
}

kreate {
    project {
        description = "What every local connection between processes in Konnect is made of: addresses, access, peers, " +
            "connections, framing and the platform primitives of Unix domain sockets and Windows named pipes."
    }
}

dependencies {
    api(libs.kore.core)
    api(libs.kollect.core)
    api(libs.konfetti.core)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.io.bytestring)
}

val testConfigurations: Set<String> = setOf("unitTestImplementation", "integrationTestImplementation")

configurations.matching { configuration -> configuration.name in testConfigurations }.configureEach {
    dependencies.add(project.dependencies.create(libs.kotlinx.coroutines.test.get()))
    dependencies.add(project.dependencies.create(libs.konfetti.testing.get()))
}

configurations.matching { configuration -> configuration.name == "unitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.project(mapOf("path" to ":konnect-testing")))
}
