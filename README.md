# Devolay — Community-Maintained Fork

Devolay is a Java binding for the NDI® SDK, providing access to NDI video, audio, metadata, discovery, sending, and receiving from Java applications through JNI.

This repository is a community-maintained fork of the original `WalkerKnapp/devolay` project created by Walker Knapp. It preserves the original Java API and package namespace while modernizing the native build, current NDI compatibility, Apple Silicon support, CI, and Maven publication infrastructure.

**NDI® is a registered trademark of Vizrt NDI AB.**

This project is independent and is not affiliated with or endorsed by Vizrt NDI AB.

## Project lineage

The original Devolay project established the Java/JNI binding architecture, public Java API, native wrappers, examples, Android support, and both separated and integrated build models.

This fork continues that work rather than replacing it.

The Java package namespace remains:

```java
me.walkerknapp.devolay
```

This is intentional and preserves source compatibility with applications written against the original Devolay API.

The Maven group used by this maintained fork is:

```text
io.github.vicvalentim
```

The current fork release line is:

```text
2.1.1-vic.1
```

## Fork goals

The purpose of this fork is to keep Devolay usable with contemporary Java, Gradle, macOS, Apple Silicon, and current NDI SDK releases while preserving compatibility with the original API wherever possible.

The fork currently focuses on:

- Apple Silicon / macOS ARM64 native support;
- compatibility with NDI SDK 6.x;
- reproducible builds without downloading proprietary NDI runtime binaries;
- modern Maven Central publication infrastructure;
- current GitHub Actions workflows;
- preservation of the original separated and integrated build architecture;
- compatibility with applications such as Processing-based audiovisual software.

## Major changes from the original upstream

### Apple Silicon support

The desktop native build now includes a native macOS `aarch64` target in addition to Intel `x86-64`.

The Java native loader recognizes both `arm64` and `aarch64` and selects:

```text
natives/macos/aarch64/libdevolay-natives.dylib
```

This has been validated natively on Apple Silicon without Rosetta.

### NDI SDK 6.3.2 header baseline

The native binding is currently compiled against vendored NDI SDK 6.3.2 headers.

The vendored headers preserve their upstream MIT license notices and allow the open-source project to compile without downloading or bundling the proprietary NDI Runtime.

Explicit SDK overrides remain available through:

```text
-DndiSdk=<SDK path>
```

or:

```text
NDI_SDK_DIR
```

### Dynamic NDI Runtime loading

The normal Devolay artifact does **not** contain the proprietary NDI runtime library.

At runtime, Devolay dynamically loads an installed NDI Runtime.

The binding intentionally retains `NDIlib_v3_load` as its dynamic ABI compatibility baseline. Current NDI 6.x runtimes continue to export this entry point.

On macOS, runtime discovery supports current NDI installations as well as the current NDI SDK for Apple layout.

The current SDK layout is:

```text
/Library/NDI SDK for Apple/lib/macOS/libndi.dylib
```

Legacy SDK locations are retained as fallbacks where appropriate.

### NDI 6.3.2 validation

The Apple Silicon implementation has been validated with:

```text
macOS 26.5.2
Apple Silicon arm64
Temurin Java 17 arm64
NDI SDK 6.3.2
NDI Runtime 6.3.2
```

`SendVideoExample` successfully transmitted video to NDI Monitor, operating predominantly near 60 fps.

The same JNI build was also validated against an installed NDI 6.0.1 runtime, demonstrating compatibility across the tested NDI 6.x runtime versions.

### Integrated build support

The original Devolay architecture supported an `integrated` artifact containing both Devolay JNI binaries and NDI runtime binaries.

This fork preserves the ability to generate integrated builds locally.

On macOS, integrated build discovery supports:

```text
-DndiSdk
NDI_SDK_DIR
/Library/NDI SDK for Apple
checkout-local NDI SDK for Apple
```

The current universal NDI macOS library can be packaged for both:

```text
macos/x86-64
macos/aarch64
```

Integrated builds are intended for controlled application development and packaging.

**This fork does not publish integrated NDI runtime binaries to Maven Central.**

See the licensing section below.

### Modernized build and CI

The fork modernizes several parts of the original build infrastructure, including:

- current GitHub Actions versions;
- Maven Central Publisher API support;
- bearer-token authentication;
- platform-specific native build jobs;
- universal desktop native artifact assembly;
- source and Javadoc publication;
- Gradle Module Metadata;
- modern `JavaExec.mainClass` configuration;
- removal of legacy NDI SDK header download jobs.

## Public artifact model

The public artifact is a **separated build**.

Conceptually:

```text
Java application
      |
      v
Devolay Java API
      |
      v
Devolay JNI native
      |
      v
dynamically loaded NDI Runtime
      |
      v
NDI network
```

The public JAR contains Devolay JNI binaries but does not contain:

```text
libndi.dylib
libndi.so
ndi.dll
```

An NDI Runtime must therefore be installed on the target system.

## Maven coordinates

The maintained fork uses:

```text
io.github.vicvalentim:devolay:2.1.1-vic.1
```

The first public Maven Central release of the fork is being prepared.

Until the release is visible in Maven Central, use Maven Local as described below.

### Gradle

After publication to Maven Central:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.vicvalentim:devolay:2.1.1-vic.1'
}
```

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.vicvalentim:devolay:2.1.1-vic.1")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.vicvalentim</groupId>
    <artifactId>devolay</artifactId>
    <version>2.1.1-vic.1</version>
</dependency>
```

## Using Maven Local

Clone the repository and publish the current fork locally:

```bash
./gradlew clean :devolay-java:publishToMavenLocal
```

Then use:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.github.vicvalentim:devolay:2.1.1-vic.1'
}
```

The artifact is installed under the normal local Maven repository for the current user.

## NDI Runtime

The separated build requires an installed NDI Runtime.

Obtain the current runtime or NDI tools from the official NDI distribution at `ndi.video`.

On current macOS installations, the runtime may normally be available as:

```text
/usr/local/lib/libndi.dylib
```

For development with a full NDI SDK installation, Devolay can also load the SDK runtime directly.

For NDI SDK 6.3.2 on macOS:

```bash
export NDI_RUNTIME_DIR_V6="/Library/NDI SDK for Apple/lib/macOS"
```

Then run the application normally.

The NDI Runtime version used at execution time can be queried through:

```java
Devolay.getNDIVersion();
```

## Building from source

Use the repository Gradle wrapper rather than a system Gradle installation.

### Basic requirements

Desktop development requires:

```text
Git
JDK
C/C++ toolchain appropriate for the target platform
```

The project retains Java 8 source compatibility.

The CI build currently uses Java 11, and Apple Silicon validation has also been performed with Java 17.

### Standard build

```bash
./gradlew clean build
```

### Build desktop native artifacts

```bash
./gradlew :devolay-natives:assembleNativeArtifacts
```

The resulting native artifact contains the JNI libraries arranged by operating system and architecture.

For macOS:

```text
natives/macos/x86-64/libdevolay-natives.dylib
natives/macos/aarch64/libdevolay-natives.dylib
```

### Apple Silicon native build

```bash
./gradlew :devolay-natives:assembleReleaseMacosAarch64
```

This builds Devolay JNI natively for Apple Silicon.

## Local integrated build

A local integrated build can be generated when the appropriate NDI SDK is installed.

For macOS:

```bash
export NDI_SDK_DIR="/Library/NDI SDK for Apple"

./gradlew :devolay-java:integratedJar
```

The resulting artifact is generated under:

```text
devolay-java/build/libs/
```

The integrated artifact combines the Devolay JNI binaries with NDI runtime binaries discovered from the installed SDK.

This capability is preserved for development, testing, and controlled application packaging.

It is **not part of this fork's public Maven Central publication**.

## Examples

Example applications are located in:

```text
examples/src/main/java/me/walkerknapp/devolayexamples
```

Available examples cover the principal Devolay sender, receiver, finder, audio, video, and metadata workflows inherited from the original project.

For example, run the video sender with:

```bash
./gradlew :examples:executeSendVideoExample
```

A compatible NDI monitor or receiver on the network should then be able to discover the sender.

## Desktop targets

The native build configuration currently includes:

| Platform | Architecture |
|---|---|
| Windows | x86 |
| Windows | x86-64 |
| Linux | x86 |
| Linux | x86-64 |
| macOS | x86-64 |
| macOS | aarch64 / Apple Silicon |

Apple Silicon has been validated directly on native ARM64 hardware.

## Android

Android support is inherited from the original Devolay project and remains present in the source tree.

Configured ABIs include:

```text
armeabi-v7a
arm64-v8a
x86
x86_64
```

Android builds require an Android NDK and the corresponding NDI SDK binaries.

Android packaging is not currently part of the maintained fork's public Maven Central desktop release pipeline.

When Android tooling is not installed, Gradle may report that Android builds are unavailable. This does not prevent normal desktop builds.

## Public Maven Central publication policy

The public Maven Central artifact intentionally contains only:

```text
Devolay Java classes
Devolay JNI desktop binaries
sources
Javadoc
POM metadata
Gradle Module Metadata
```

It intentionally excludes:

```text
NDI runtime binaries
integrated NDI artifacts
Android AAR artifacts
```

This separation keeps the open-source Java/JNI binding distinct from the proprietary NDI runtime distribution.

## Licensing

### Devolay source code

Devolay is distributed under the Apache License 2.0.

The original project copyright and attribution are preserved.

### Vendored NDI headers

The NDI headers vendored into this repository retain their upstream license notices.

The NDI SDK documentation permits header files to be included in open-source projects under the terms applicable to those headers.

### NDI runtime binaries

NDI runtime binaries are **not** licensed under the Devolay Apache License.

They remain subject to the NDI SDK License Agreement and applicable third-party license terms.

The public Maven Central artifacts produced by this fork do not redistribute those runtime binaries.

Local integrated builds may contain NDI binaries obtained from a locally installed SDK. Anyone distributing an application containing those binaries is responsible for complying with the current NDI SDK License Agreement, redistribution requirements, trademark requirements, and applicable third-party rights.

Refer to the current documentation and license materials distributed by NDI before redistributing an integrated application.

## NDI trademark requirements

Applications using NDI should follow the current identification and trademark requirements published by Vizrt NDI AB.

The official NDI information and developer resources are available at:

```text
ndi.video
```

NDI® is a registered trademark of Vizrt NDI AB.

## Original project

This repository is derived from and remains technically indebted to the original Devolay project by Walker Knapp:

```text
WalkerKnapp/devolay
```

The original project established the Java API, JNI implementation, examples, native build model, integrated build concept, and initial multi-platform support on which this maintained fork is based.

The maintained fork is available as:

```text
vicvalentim/devolay
```

## Compatibility philosophy

The fork follows three principles:

```text
Preserve the Java API whenever possible.
Modernize build and platform support without unnecessary breaking changes.
Keep proprietary NDI runtime distribution separate from the public open-source artifact.
```

For that reason, the Maven group has changed while the Java package namespace remains compatible with the original library.

## Contributing

Bug reports and pull requests are welcome.

When reporting native-loading issues, include:

```text
operating system
CPU architecture
Java version
NDI Runtime version
NDI SDK version, if applicable
relevant Gradle task
complete native-loading error
```

For macOS Apple Silicon issues, also include the result of:

```bash
uname -m
java -version
```

## Acknowledgements

Walker Knapp — original Devolay author and architecture.

Vizrt NDI AB — NDI SDK and technology.

Community contributors to the original Devolay project and this maintained fork.