# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/konnect/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected Konnect version, module and JDK,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Scope

Konnect is a library: it runs with the privileges of the application that uses it. It treats everything it reads
from outside the program as untrusted: address, netmask, network, interface, range and port texts, prefix lengths
and the octets and groups handed to a factory.

The following are **in scope**:

- input that makes Konnect use unbounded memory or CPU, overflow, or crash with an unexpected exception,
- untrusted text that reaches a message, a report or a `toString`, so it can forge log lines or control a terminal,
- a parser that accepts what it documents as refused, such as an IPv4 octet with a leading zero, which other
  software reads as octal, or a network with host bits set,
- a classification that is wrong in a way that weakens a check, such as a private, loopback or link-local address
  that `AddressCategory` reports as public,
- arithmetic that overflows or wraps, such as the count of a range, the next network or a port range.

The following are **not** vulnerabilities in Konnect:

- a vulnerability in the JDK, the Kotlin standard library, `kotlinx.coroutines` or Kore — report those to the
  respective project,
- an application that trusts an address it read from a header of a request it did not authenticate,
- the resolution of host names; Konnect never resolves a name or opens a connection,
- `konnect-testing` on a production class path; its values are fixed by design.

## Known Limitations

- `AddressCategory` follows the IANA special-purpose registries at the time of a release. A newer registry entry
  needs a newer release of Konnect.
- Konnect reads only the canonical forms it documents. IPv6 zone identifiers, such as `fe80::1%eth0`, and the
  shorthand IPv4 forms of `inet_aton`, such as `127.1`, are refused.

## Verifying a Release

Releases are published to `https://repo.davils.com/releases` and signed with the release key of Davils; its
fingerprint is published in this section with the first release, so a consumer can pin it as a `trusted-key` in its
own `gradle/verification-metadata.xml`. A release also carries a signed build provenance attestation made by the
GitHub Actions run that built it:

```bash
gh attestation verify konnect-core-jvm-1.1.0.jar --repo davils-com/konnect --signer-repo davils-com/kotlin-workflow
```

Snapshots in `https://repo.davils.com/snapshots` are neither signed nor attested and are not meant for production.
