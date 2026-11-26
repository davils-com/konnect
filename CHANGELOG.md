# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - 2026-11-26

### Added

- **`konnect-ipc-shutdown`, the binding to the coordinated shutdown of Knot.** `ipcServer(server)` closes an IPC server
  in the stop phase and `ipcHttpServer(engine, gracePeriod)` drains an HTTP engine over IPC in the drain phase, both
  within the time the shutdown has left.
- **A connect retry for clients that wait for a local server.** `connectRetry { maxAttempts = 5 }` retries a connect
  that finds no server, is not accepted in time or finds the server unavailable through the retry of kontinuity, with
  its backoff, jitter, budget, events and meters; a server that is not trusted or refuses access is never tried again.
- **Converters of Kore for ports, addresses and networks.** `PortConverters.port`, `PortConverters.portRange`,
  `AddressConverters.ipAddress` and `AddressConverters.ipNetwork` read them typed in the parameters of Kore, the
  environment of Knot and the properties of Kontext, refusing everything else with constant text.

### Changed

- **The events of the IPC client and server reach the bus through the `Emitter` of konfetti 1.1.0.**
- **The token of a staged socket and the environment of the IPC transports come from Kore 2.5.0**, `SecureEntropy` and
  `processVariable`, instead of an own `SecureRandom` and `System.getenv`.
- **Built with Kreate 5.5.2 and Gradle 9.9.2 on Kore 2.5.0, konfetti 1.1.0, kollect 1.1.0, kontinuity 1.1.0 and Knot
  1.2.0**, with Ktor 3.6.1, kotlinx-io 0.9.3 and the version floors of the build tools raised.

## 1.0.0 - 2026-11-25

The first version of Konnect: the network library of Davils as an open source library under the Apache License 2.0. It
reads and computes ports, IPv4 and IPv6 addresses, netmasks, networks, interfaces and ranges strictly, for Kotlin on the
JVM and on `wasmJs`, and connects processes on one machine over Unix domain sockets and Windows named pipes on the JVM,
with a Ktor client engine and a Ktor server engine that speak HTTP/1.1 over these connections. It is built with Kreate
5.5.2 on Kore 2.3.2, Kollect 1.0.0 and Konfetti 1.0.0 and published to `https://repo.davils.com`, the native library of
the named pipes per platform for Windows on x64 and ARM64. The code comes from the network and IPC libraries of an
internal Davils project and is hardened for critical infrastructure on the way.

### Added

- **The build of Konnect.** Kotlin Multiplatform modules for `jvm` and `wasmJs`, `konnect-core`,
  `konnect-internet-protocol` and `konnect-testing`, the JVM modules `konnect-ipc-core`, `konnect-ipc-client` and
  `konnect-ipc-server` under `konnect-ipc/`, and the BOM `konnect-bom`, built, checked and published like Kore on
  Kore 2.3.2 and Kreate 5.5.2.
- **Ports.** `konnect-core` holds `Port`, a value class over a number from `0` to `65535` with its `PortCategory`
  of RFC 6335, and `PortRange`, a range of ports that merges, iterates and is built in the `portRange { }` DSL.
  Both are read from text with `parse`, `parseOrNull`, `toPort` and `toPortRange`, which never quote the text they
  refuse, and inside a validation of Kore with `port` and `portRange`, which report the codes of
  `PortViolationCodes`. `Limits.MAX_TEXT_LENGTH` bounds every text Konnect reads; each module keeps its own
  tighter bounds in a `Limits` object of its root package.
- **The internet protocol.** `konnect-internet-protocol` holds IPv4 and IPv6 addresses with their
  `AddressCategory` from the IANA special-purpose registries as of 2025-10-09, netmasks, networks and interfaces in CIDR notation,
  ranges of addresses, subnetting, supernetting and the aggregation of networks, ported from the network library of
  Kide onto the validation of Kore. Every value is read with `parse`, `parseOrNull` and the `String` extensions,
  which never quote the text they refuse, and inside a validation with the extensions of `IpValidations`, which
  report the codes of `IpViolationCodes`. Networks, interfaces and ranges are also built in DSLs.
- **Tunnel addresses are never public.** Teredo `2001::/32` and 6to4 `2002::/16` have the new category
  `AddressCategory.Tunnel` and are not globally reachable, because they carry an IPv4 address that may be private
  or a loopback address; Kide counted 6to4 as public. The AS112, AMT, 6a44, service continuity, dummy and NAT64
  discovery blocks that Kide lacked have their categories as well.
- **Exact address counts.** `AddressCount` holds the number of addresses of a range or a network exactly, up to the
  `2^128` addresses of the whole IPv6 space, on the JVM and on Wasm; `toBigInteger()` turns it into a
  `java.math.BigInteger` on the JVM.
- **Local connections between processes.** `konnect-ipc-core`, `konnect-ipc-client` and `konnect-ipc-server` port
  the IPC of Kide onto Kore, Kollect and Konfetti, with HTTP inside client and server instead of modules of its own:
  addresses, access and peer checks over Unix domain sockets and Windows named pipes, length-prefixed framing,
  `ipcClient { }` and `ipcServer { }`, the Ktor client engine `IpcHttp` and the Ktor server engine `IpcHttpServer`.
  Spans, meters and events replace every log line of Kide; nothing is logged.
- **Hardened beyond Kide.** A Unix socket is bound in a private staging directory and moved into place, so it never
  exists with default rights; no error, reason or HTTP answer quotes what a peer sent; response and request heads,
  bodies, chunk lines, pipelined requests, idle and body timeouts and connections are bounded by the `Limits` of each
  module; the native library checks every buffer and argument and never throws across JNI, and includes the header
  Kreate generates, which Kide named wrongly; a failed traced block ends its span as an error, not as cancelled.
- **Native named pipes.** The C++ library of `konnect-ipc-core` is built with Kreate's JNI support and published per
  platform as `konnect-ipc-core-windows-x86_64` and `konnect-ipc-core-windows-aarch64`. It is built with MSVC only,
  which the build enforces, with every warning as an error, clang-tidy, control flow guard and Spectre mitigation, CET
  shadow stacks on x64 and signed return addresses on ARM64.
- **Test values.** `konnect-testing` holds `TestAddresses` and `TestNetworks` from the documentation ranges of RFC
  5737 and RFC 3849 and `TestPorts` from the dynamic range of RFC 6335. On the JVM it adds `TestIpc` with in-memory IPC connections,
  `ScriptedConnector` and the fakes of the IPC transports.
