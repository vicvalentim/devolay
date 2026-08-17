# Devolay Release Checklist

This checklist applies to releases of the community-maintained Devolay fork.

The public distribution model is runtime-separated by default.

The normal Maven artifact contains the Devolay Java API and Devolay JNI native libraries. It must not contain proprietary NDI runtime binaries.

The current repository uses the WalkerKnapp Gradle 7.2cc wrapper for native build support. Release and verification commands in this checklist must be run with JDK 11 unless the Gradle/native build infrastructure is explicitly upgraded in a later release. This build-JVM requirement does not change Devolay's Java 8 source compatibility.

## 1. Verify repository state

Confirm that the release branch is current and the working tree is clean.

Run `git status` and review the recent commit history before beginning a release.

## 2. Verify current NDI SDK and licensing

Before every public release:

1. Check the current NDI SDK version at the official NDI developer site.
2. Review the current NDI SDK License Agreement.
3. Review the current SDK software-distribution requirements.
4. Review current NDI identification and trademark requirements.
5. Compare the current SDK with the headers vendored by Devolay.

Do not assume that conclusions made for an earlier release remain valid for a later NDI SDK or license revision.

The NDI SDK License Agreement requires product releases to use a sufficiently current SDK when a newer version is available. The current official materials must therefore be checked as part of every release.

## 3. Verify normal build separation

Run:

    ./gradlew clean build

The normal build must succeed without discovering or adding an NDI runtime to an integrated artifact.

Messages indicating that `libndi`, `Processing.NDI.Lib`, or another NDI runtime is being added during a normal build are release blockers.

## 4. Verify Maven Local publication

Run:

    ./gradlew clean :devolay-java:publishDevolayPublicationToMavenLocal

Inspect the generated standard artifact.

It must contain the applicable Devolay JNI native libraries.

It must not contain:

- `libndi.dylib`;
- `libndi.so`;
- `Processing.NDI.Lib.x64.dll`;
- `ndi.dll`;
- an integrated NDI classifier.

## 5. Verify external Maven consumer

Create or use an independent consumer project resolving:

    io.github.vicvalentim:devolay:<version>

The consumer test should verify that it can:

1. resolve Devolay from Maven Local;
2. load the appropriate Devolay JNI library;
3. load an independently installed NDI Runtime;
4. report the NDI runtime version;
5. create a `DevolaySender`.

This test must not depend on the Devolay repository's build classpath.

## 6. Verify optional integrated packaging

Integrated packaging must remain explicit opt-in.

Without the flag:

    ./gradlew clean :devolay-java:integratedJar

no integrated JAR should be produced.

With explicit opt-in:

    ./gradlew clean -PenableIntegratedNdi=true :devolay-java:integratedJar

the integrated artifact may be generated from a complete locally installed or explicitly supplied NDI SDK.

Both the runtime binary and corresponding NDI license file are mandatory. Missing either must fail the build.

## 7. Verify integrated CI guard

Integrated NDI packaging must fail in CI by default.

A deliberately controlled/private environment may enable it with both:

    -PenableIntegratedNdi=true
    -PallowIntegratedNdiInCi=true

The override must never be added to ordinary public publication workflows.

## 8. Desktop integrated target status

The maintained 64-bit desktop integrated configuration covers:

- macOS x86-64;
- macOS aarch64 / Apple Silicon;
- Windows x86-64;
- Linux x86-64.

A target must not be documented as validated until it has been tested on the corresponding operating system with a current NDI SDK.

Legacy 32-bit native Devolay targets do not imply maintained 32-bit integrated NDI support.

Android follows a separate build and packaging path and must be reviewed separately before any public Android artifact is introduced.

## 9. macOS validation

On Apple Silicon, verify the current architecture with `uname -m`.

Build the Apple Silicon target and run the sender example against the current NDI Runtime.

Confirm actual discovery and reception with an NDI receiver such as NDI Monitor.

When testing the integrated build, inspect the resulting JAR and confirm the expected macOS Intel and Apple Silicon runtime, license, and Devolay JNI entries.

## 10. Windows and Linux validation

Before claiming validated integrated support for Windows x86-64 or Linux x86-64:

1. use a complete current NDI SDK for that platform;
2. run the integrated build on that operating system;
3. inspect the resulting package;
4. run an actual Devolay sender or receiver;
5. verify communication with another NDI application;
6. record the SDK version used for validation.

Do not substitute cross-compilation success for runtime validation.

## 11. Maven Central staging

Run the Maven Central staging workflow before any public release upload.

The staging pipeline must verify:

- Linux native build;
- Windows native build;
- macOS Intel native build;
- macOS Apple Silicon native build;
- universal public native artifact assembly;
- source and Javadoc artifacts;
- POM validation;
- signatures;
- Maven Central/JReleaser validation;
- absence of proprietary NDI runtime binaries.

Any validation failure is a release blocker.

## 12. Maven Central upload and publication

`.github/workflows/central-staging.yml` is the non-publishing qualification workflow.

`.github/workflows/central-release.yml` performs the authenticated Maven Central upload using JReleaser with `stage: UPLOAD`.

With JReleaser 1.25.0, the `UPLOAD` stage uploads the bundle and waits for the Central deployment to reach `VALIDATED` or `FAILED`. It does not invoke the Maven Central publication endpoint.

The JReleaser 1.25.0 Maven Central client uploads the bundle without specifying `publishingType`. Under the Central Publisher API contract, the omitted value defaults to `USER_MANAGED`. A successfully validated deployment therefore remains in `VALIDATED` until a separate publication action is explicitly performed.

Treat the process as two distinct authorization boundaries:

1. **Upload authorization** — run `.github/workflows/central-release.yml`, producing a validated Central deployment and recording its `deploymentId`.
2. **Publication authorization** — explicitly publish that validated deployment, either through the Central Portal or through a controlled JReleaser `PUBLISH` operation using the recorded `deploymentId`.

Before publication, inspect the validated deployment and confirm that all release, binary architecture, signing, packaging, licensing, and physical runtime qualification gates have passed.

Once publication is explicitly authorized, the transition from `VALIDATED` to `PUBLISHING` and then `PUBLISHED` is irreversible for that released Maven version.

After Maven Central reports the deployment as `PUBLISHED`:

1. verify the public Maven coordinates;
2. create the Git tag at the exact qualified release commit;
3. create the corresponding GitHub Release;
4. update current-version documentation in a subsequent post-release commit.

## 13. Signing

Confirm that the release signing key is valid, unexpired, correctly configured in GitHub Actions, and discoverable by the required validation infrastructure.

Never commit signing keys, private-key material, passwords, passphrases, or credentials to the repository.

## 14. Maven Central credentials

Maven Central publication credentials must remain in GitHub Actions secrets or protected environment secrets.

Central User Tokens, bearer tokens, usernames, passwords, and signing credentials must never be stored in repository files.

Keep Maven Central upload and publication actions uninvoked while preparing or auditing a release.

## 15. Final release gate

Before authorizing a public Maven Central publication, confirm all of the following:

- repository build passes;
- working tree and release commit are known;
- current NDI SDK version has been checked;
- current NDI licensing and distribution documentation has been reviewed;
- normal build performs no integrated NDI discovery;
- standard Maven artifact contains no NDI runtime binary;
- integrated packaging remains explicit opt-in;
- missing integrated runtime fails the build;
- missing integrated license file fails the build;
- integrated CI guard is active;
- README licensing information is current;
- `THIRD_PARTY_NOTICES.md` is current;
- Maven Local publication passes;
- external consumer test passes;
- applicable runtime tests pass;
- Central staging validation passes;
- signing is valid;
- Maven Central credentials are current.

Only after all release gates pass should the Maven Central upload be authorized. Publication requires a separate explicit authorization.
