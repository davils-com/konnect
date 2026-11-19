# AGENTS.md

Binding rules for every agent and every person working in this repository. Konnect is the network library of
Davils: strict, attacker-proof building blocks for ports, IPv4 and IPv6 addresses, netmasks, networks, interfaces
and ranges in Kotlin on the JVM and on WebAssembly (`wasmJs`). It is built on Kore, the foundation library of Davils,
and follows its rules.
It is published under the Apache License 2.0 and is built to the standard it is used for, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `konnect-core` | `com.davils:konnect-core` | What every network module shares: ports, port ranges and their categories, the limits of Konnect |
| `konnect-internet-protocol` | `com.davils:konnect-internet-protocol` | IPv4 and IPv6 addresses, netmasks, networks, interfaces, ranges, address counts and the aggregation of networks |
| `konnect-testing` | `com.davils:konnect-testing` | Test support for code built on Konnect: documentation addresses, networks and ports; never on a production class path |
| `build-logic` | - | One convention plugin per Kreate concern, composed by `konnect.kotlin-library` for the modules and `konnect.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.konnect.<module without konnect- and without hyphens>`, such as
`com.davils.konnect.core` or `com.davils.konnect.internetprotocol`, and its Java module, named as
`Automatic-Module-Name`, has the same name.

Validation, violations, builders and the `Davils*` annotations come from Kore (`com.davils.kore.core`); Konnect never
builds a second validation layer.

## Targets

Every module is a Kotlin Multiplatform library with two targets: `jvm` and `wasmJs`. The Wasm tests run under Node.js;
the published klib runs in the browser as well.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM can offer, such as files, paths,
  URIs, threads, native libraries and the types of the JDK, lives in `src/jvmMain/kotlin`; a member of a common class
  that needs a JDK type becomes an extension function there.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`. Konnect declares no `expect` or `actual` class: they are a Beta language feature.
- JavaScript interop is written with `js("...")` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and
extra warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and
`allRules`, both test modules, Kover verification at 97 % of lines, branches and instructions, the API and semver
check, the license header check, the changelog check, every Trivy scan and the Kreate assurance gate, which blocks
on every finding. The gate reads OSV.dev and deps.dev and Trivy downloads its database, so `check` needs the
network. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it and toolchains are
never downloaded.

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without
a baseline. Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **One top-level type per file**, named after it; a file of functions is named after its role.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead, and when a subpackage shrinks to one file, that file moves up and the
  subpackage goes away. Exceptions are the root package of a module and an `internal` package, which may hold a
  single file.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `SemanticVersion`,
  never `KonnectSemanticVersion`. The package says where a declaration comes from. Where a name meets a type of the same
  name, the foreign type is imported with an alias, such as `import kotlin.uuid.Uuid as KotlinUuid`, and a KDoc link
  names it in full. The only prefix is `Davils` on the annotations shared by every Davils library
  (`DavilsInternal`, `DavilsExperimental`, `DavilsDsl`, `DavilsGenerated`, `DavilsPlatformBinding`). The ids of the
  Gradle plugins in `build-logic` keep their `konnect.` prefix.
- **`@DavilsInternal`** marks API shared between Davils libraries that consumers must not use; it fails the build of
  every caller that does not opt in. **`@DavilsExperimental`** marks API that may still change.
- **Immutable values.** Public types are immutable; public collections are built with `immutableList()`,
  `buildList` and the like, never stored from `toList()` or `+`, so a cast cannot change them.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString`; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent. Atomics come from `kotlin.concurrent.atomics`, through
  a local `@OptIn(ExperimentalAtomicApi::class)`.
  Experimental Kotlin API is allowed only through a local `@OptIn` and never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no threads or timers of the library's own.
  Detekt does not enforce this rule; reviews do.

## Security rules

Every input of Konnect may come from an attacker: address, network, range and port texts, prefix lengths and
the octets and groups handed to a factory. Konnect is built so that a mistake of a caller fails closed.

- **Limits before work.** Every entry point checks its input against `Limits` before it does work that grows
  with the input, and refuses what is too large with a violation or an exception. Nothing is truncated silently.
  A new limit goes into `Limits`, never into a private constant.
- **Never echo input.** A message, a reason or a report says what is wrong in constant text and never quotes the
  text it refused. Canonical values with a restricted alphabet, such as addresses, networks and ports, are shown as
  they are.
- **Fail closed.** Text that is not exactly one canonical or accepted form is refused: no leading zeros that could
  be read as octal, no zone identifiers, no whitespace, no partial reads. A network with host bits set is an error,
  not silently masked.
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`.
- **Every sink has an injection test.** Every parser is tested with hostile text, and a property test shows that
  no message it produces contains the input.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from `konnect-testing` or a `fixture`.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test,
  even when that package holds a single test. A test that needs the JVM, through reflection, threads, files, Java
  serialization or a JDK type, lives in `src/jvmUnitTest/kotlin`; a test of common code stays common wherever it can. A fixture lives in the `fixture` folder of the only subpackage that uses it, or in the shared `fixture` folder
  of the area; a `fixture` folder holds at least two files, so a lone fixture lives next to the tests that use it.
  `src/test` is rejected by the build.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("konnect.kotlin-library")`, and declares nothing but its description
  and its dependencies, through the configurations of the source sets such as `commonMainApi`. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured only in
  `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one Kreate concern and applies `konnect.base`:

  | Plugin | Configures |
  |---|---|
  | `konnect.base` | The Kreate plugin, the group and the version |
  | `konnect.dependency-locking` | Strict locking of every configuration |
  | `konnect.git-hooks` | The Git hooks |
  | `konnect.trivy` | Every Trivy scan |
  | `konnect.assurance` | The assurance gate and its `ignore` rules |
  | `konnect.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `konnect.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `konnect.testing` | Kotest and the test suites |
  | `konnect.detekt` | Detekt and its reports |
  | `konnect.coverage` | Kover and its verification |
  | `konnect.api-validation` | The API dump and the semver check |
  | `konnect.docs` | Dokka |
  | `konnect.publish` | The POM, the release guard and the Davils repository |
  | `konnect.archives` | The legal files and the manifest of every archive |
  | `konnect.tool-classpaths` | The version floors on the class paths of the build tools |
  | `konnect.changelog` | The changelog check |
  | `konnect.license-header` | The license header check |

  `konnect.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `konnect.root` composes them for the root project. A new concern gets a plugin of its own, never a block in a
  composing plugin.
- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains may be added when they are
  needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- Konnect builds only with released versions of Kreate and Kore. `gradle/verification-metadata.xml` trusts `com.davils` artifacts
  only by the signature of the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063`; a snapshot of Kreate or Kore is
  never put into the catalog, and no trust rule for unsigned snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/konnect.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every publication,
refuses a build whose resolved version differs from it, so no environment variable can change what is published. Every build without a release tag is a
`-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`, that equals that version, sits on a commit of `main`
and is built without any snapshot tooling; the guard refuses every other combination before anything is published.
Snapshots go to `https://repo.davils.com/snapshots`, releases to `https://repo.davils.com/releases`, and never to
Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in `DAVILS_MAVEN_NAME`
and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in `ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`;
a release without a signing key fails.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(core): ...`, `fix(internet-protocol): ...` or `test(testing): ...`. Small commits, one concern each. Commit only when asked. No
`Co-Authored-By` trailer and no other attribution to tools or AI assistants.
