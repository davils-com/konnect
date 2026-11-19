# Third-Party Material

No published artifact of Konnect contains third-party code.

The tables of special-purpose addresses follow the IANA IPv4 and IPv6 Special-Purpose Address Registries
(RFC 6890), and the documentation addresses follow RFC 5737 and RFC 3849. They are facts, not copied material.

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.
