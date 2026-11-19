# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- **The build of Konnect.** Three Kotlin Multiplatform modules for `jvm` and `wasmJs`, `konnect-core`,
  `konnect-internet-protocol` and `konnect-testing`, built, checked and published like Kore.
- **Ports.** `konnect-core` holds `Port`, a value class over a number from `0` to `65535` with its `PortCategory`
  of RFC 6335, and `PortRange`, a range of ports that merges, iterates and is built in the `portRange { }` DSL.
  Both are read from text with `parse`, `parseOrNull`, `toPort` and `toPortRange`, which never quote the text they
  refuse, and inside a validation of Kore with `port` and `portRange`, which report the codes of
  `PortViolationCodes`. `Limits.MAX_TEXT_LENGTH` bounds every text Konnect reads.
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
- **Test values.** `konnect-testing` holds `TestAddresses` and `TestNetworks` from the documentation ranges of RFC
  5737 and RFC 3849 and `TestPorts` from the dynamic range of RFC 6335.
