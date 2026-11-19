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
  `PortViolationCodes`. `Limits` bounds every text Konnect reads and every list it builds at once.
