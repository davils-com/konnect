# Konnect

Konnect is the network library of Davils: strict building blocks for ports, IPv4 and IPv6 addresses, netmasks,
networks, interfaces and ranges in Kotlin on the JVM and on WebAssembly (`wasmJs`), safe to feed with untrusted
input. It is built on [Kore](https://github.com/davils-com/kore) and published under the
[Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `konnect-core` | `com.davils:konnect-core` | Ports, port ranges and their categories |
| `konnect-internet-protocol` | `com.davils:konnect-internet-protocol` | IPv4 and IPv6 addresses and their categories, netmasks, networks, interfaces, ranges, address counts and the aggregation of networks |
| `konnect-testing` | `com.davils:konnect-testing` | Documentation addresses, networks and ports for tests; keep it out of production class paths |

Konnect is a Kotlin Multiplatform library for two targets, `jvm` and `wasmJs`, and is written for Kotlin 2.4 or
newer. Its only runtime dependency is `kore-core`. On the JVM it requires JDK 25.

## Installation

Konnect is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:konnect-internet-protocol:1.0.0")
    testImplementation("com.davils:konnect-testing:1.0.0")
}
```

Snapshots of the `develop` branch are in `https://repo.davils.com/snapshots`. They are neither signed nor attested
and not meant for production.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access. The rules for code, tests and the build are in [AGENTS.md](AGENTS.md), how a
change gets in is in [CONTRIBUTING.md](CONTRIBUTING.md), and how to report a vulnerability is in
[SECURITY.md](SECURITY.md).
