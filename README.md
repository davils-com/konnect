# Konnect

Konnect is the network library of Davils: strict building blocks for ports, IPv4 and IPv6 addresses, netmasks,
networks, interfaces and ranges in Kotlin on the JVM and on WebAssembly (`wasmJs`), and local connections between
processes over Unix domain sockets and Windows named pipes on the JVM, safe to feed with untrusted input. It is built on [Kore](https://github.com/davils-com/kore) and published under the
[Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `konnect-core` | `com.davils:konnect-core` | Ports, port ranges and their categories |
| `konnect-internet-protocol` | `com.davils:konnect-internet-protocol` | IPv4 and IPv6 addresses and their categories, netmasks, networks, interfaces, ranges, address counts and the aggregation of networks |
| `konnect-ipc-core` | `com.davils:konnect-ipc-core` | Local connections: addresses, access, peers, connections, length-prefixed framing and telemetry, over Unix domain sockets and Windows named pipes |
| `konnect-ipc-client` | `com.davils:konnect-ipc-client` | Connecting to a local server and checking who it is, and `IpcHttp`, a Ktor client engine for HTTP/1.1 over the connection |
| `konnect-ipc-server` | `com.davils:konnect-ipc-server` | Binding, accepting and checking peers, and `IpcHttpServer`, a Ktor server engine for HTTP/1.1 over the connection |
| `konnect-ipc-shutdown` | `com.davils:konnect-ipc-shutdown` | Optional: closes an IPC server and drains an HTTP engine over IPC in their phases of a shutdown of [Knot](https://github.com/davils-com/knot) |
| `konnect-testing` | `com.davils:konnect-testing` | Documentation addresses, networks and ports for tests, and on the JVM in-memory IPC connections and a scripted connector; keep it out of production class paths |
| `konnect-bom` | `com.davils:konnect-bom` | One version for every Konnect module |

`konnect-core`, `konnect-internet-protocol` and `konnect-testing` are Kotlin Multiplatform libraries for `jvm` and
`wasmJs` whose only runtime dependency is `kore-core`. The IPC modules are JVM libraries; they build on Kore, report
through [Kollect](https://github.com/davils-com/kollect) and [Konfetti](https://github.com/davils-com/konfetti), and the
client and the server bring Ktor. Konnect is written for Kotlin 2.4 or newer and requires JDK 25 on the JVM.

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
    implementation("com.davils:konnect-internet-protocol:1.2.0")
    testImplementation("com.davils:konnect-testing:1.2.0")
}
```

On Windows, `konnect-ipc-core` needs the native library of its platform on the runtime class path; its POM names
none, so no build receives a binary it cannot use:

```kotlin
dependencies {
    implementation("com.davils:konnect-ipc-client:1.2.0")
    runtimeOnly("com.davils:konnect-ipc-core-windows-x86_64:1.2.0")
}
```

Linux and macOS need nothing more; there the JDK reaches Unix domain sockets on its own.

Snapshots of the `develop` branch are in `https://repo.davils.com/snapshots`. They are neither signed nor attested
and not meant for production.

## Local connections

A server binds an address, checks the user of every peer against its access and hands out a connection; a client
connects and checks the user of the server against its trust before it sends a byte. On Linux and macOS the
connection is a Unix domain socket, bound so that it never exists with wider rights than configured; on Windows it is
a named pipe with a protected DACL that rejects remote clients, reached through a small hardened C++ library that
ships as `com.davils:konnect-ipc-core-windows-x86_64` and `-windows-aarch64`.

```kotlin
val address = IpcAddress.Named(IpcName("agent"))

val server = ipcServer(Dispatchers.IO) {
    this.address = address
    access { allow(IpcUser.UnixUser("deploy")) }
}
server.bind()
val accepted = server.accept()

val client = ipcClient(Dispatchers.IO) { this.address = address }
val connected = client.connect()
```

A connection is a stream of bytes; `IpcFraming.lengthPrefixed()` turns it into a stream of messages for a protocol of
your own. For HTTP, the Ktor engines speak HTTP/1.1 over the same connection, keep connections alive and switch
protocols, and every call knows its peer:

```kotlin
embeddedServer(IpcHttpServer, serverConfig { module { routing { get("/whoami") {
    call.respondText(call.attributes[IpcHttpCallAttributes.peer].toString())
} } } }) { server = ipcServer(Dispatchers.IO) { this.address = address } }.start()

val http = HttpClient(IpcHttp) { engine { connector = ipcClient(Dispatchers.IO) { this.address = address } } }
http.get("http://agent/whoami")
```

Neither side logs. Spans, meters and events go to the tracer, the meter registry and the event bus of an
`ipcTelemetry { }` handed to the builder; a rejected peer is counted and published, never written to a log.

## Building

```bash
./gradlew check
```

It needs JDK 25, CMake, Ninja and network access; the C++ library is compiled only on Windows, with MSVC and
clang-tidy. The rules for code, tests and the build are in [AGENTS.md](AGENTS.md), how a
change gets in is in [CONTRIBUTING.md](CONTRIBUTING.md), and how to report a vulnerability is in
[SECURITY.md](SECURITY.md).
