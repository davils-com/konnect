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
    id("konnect.jvm-library")
}

kreate {
    project {
        description = "The client side of local connections in Konnect: connecting to a Unix domain socket or a Windows" +
            " named pipe, checking the server and speaking HTTP/1.1 over it through a Ktor client engine."
    }
}

dependencies {
    api(project(":konnect-ipc:konnect-ipc-core"))
    api(libs.ktor.client.core)
    api(libs.kotlinx.io.core)
    api(libs.kontinuity.retry)
    implementation(libs.ktor.http.cio)
}

val testConfigurations: Set<String> = setOf("unitTestImplementation", "integrationTestImplementation")

configurations.matching { configuration -> configuration.name in testConfigurations }.configureEach {
    dependencies.add(project.dependencies.create(libs.kotlinx.coroutines.test.get()))
    dependencies.add(project.dependencies.create(libs.konfetti.testing.get()))
}

configurations.matching { configuration -> configuration.name == "unitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.project(mapOf("path" to ":konnect-testing")))
}

configurations.matching { configuration -> configuration.name == "integrationTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.create(libs.ktor.server.cio.get()))
}
