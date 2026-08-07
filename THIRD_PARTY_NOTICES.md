# Third-Party Notices

This document summarizes third-party licensing and distribution considerations relevant to the community-maintained Devolay fork.

## Devolay

Devolay was originally created by Walker Knapp.

The Devolay source code is distributed under the Apache License 2.0. The original project history, copyright notices, attribution, and license are preserved by this fork.

See `LICENSE` for the complete Apache License 2.0 text.

## NDI SDK headers

This repository contains selected NDI SDK header files required to compile the open-source JNI binding.

Those files retain the copyright and license notices supplied with them by Vizrt NDI AB. The vendored headers that permit open-source redistribution retain their applicable MIT license notices in the files themselves.

Those header licenses do not place the proprietary NDI runtime or other NDI SDK components under the Devolay Apache License.

## NDI runtime binaries

NDI runtime binaries are proprietary components of the NDI SDK and are not licensed under the Devolay Apache License.

Examples include platform runtime libraries such as:

- `Processing.NDI.Lib.x64.dll`
- `libndi.dylib`
- `libndi.so`

NDI runtime binaries are not stored in this repository and are not included in the standard public Maven artifact produced by this fork.

The standard Devolay distribution uses runtime-separated loading, allowing the NDI Runtime to be installed or supplied independently according to the applicable NDI terms.

## Optional integrated packaging

This fork preserves the historical Devolay integrated-build capability for local development, testing, and controlled application packaging.

Integrated NDI packaging is explicit opt-in through the Gradle property `enableIntegratedNdi`.

The integrated build resolves a complete NDI SDK supplied or installed by the developer and requires both the platform runtime binary and its accompanying NDI license file.

The maintained integrated desktop configuration targets:

- macOS x86-64;
- macOS aarch64 / Apple Silicon;
- Windows x86-64;
- Linux x86-64.

macOS x86-64 and Apple Silicon packaging have been directly validated with NDI SDK 6.3.2. Windows x86-64 and Linux x86-64 remain subject to platform-specific validation.

Integrated packaging is disabled in CI by default. Controlled CI may override this guard explicitly using `allowIntegratedNdiInCi`.

This fork does not publish integrated NDI runtime binaries to Maven Central or as public GitHub Actions artifacts.

## Application distribution

The NDI SDK License Agreement and SDK documentation govern distribution of applications containing NDI runtime binaries.

A developer or distributor using the local integrated build is responsible for ensuring that the resulting application satisfies the current NDI SDK License Agreement, software-distribution requirements, identification requirements, trademark requirements, applicable third-party licenses, and any required end-user license terms.

NDI licensing requirements may change independently of this project. The current NDI materials should therefore be reviewed before every product release.

Official information is available at https://ndi.video/.

## Trademark

NDI® is a registered trademark of Vizrt NDI AB.

Devolay and this community-maintained fork are independent projects and are not affiliated with or endorsed by Vizrt NDI AB.
