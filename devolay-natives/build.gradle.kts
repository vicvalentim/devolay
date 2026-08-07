import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import java.nio.file.Files
import java.nio.file.Path

plugins {
    `cpp-library`
    id("de.undercouch.download") version "4.0.4"
}

apply {
    from("gradle/toolchains.gradle.kts")
}

// Download gulrak/filesystem to replace c++17's filesystem if not supported by the current system.
val downloadNativeDependencies by tasks.registering(Download::class) {
    src("https://github.com/gulrak/filesystem/releases/download/v1.3.2/filesystem.hpp")
    dest(temporaryDir)

    outputs.file(temporaryDir.resolve("filesystem.hpp"))
    outputs.dir(temporaryDir)
}

val downloadJniHeader by tasks.registering(Download::class) {
    src("https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/share/native/include/jni.h")
    dest(temporaryDir)
    outputs.dir(temporaryDir)
}

val downloadJniMdHeaderUnix by tasks.registering(Download::class) {
    src("https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/unix/native/include/jni_md.h")
    dest(temporaryDir)
    outputs.dir(temporaryDir)
}

val downloadJniMdHeaderWindows by tasks.registering(Download::class) {
    src("https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/windows/native/include/jni_md.h")
    dest(temporaryDir)
    outputs.dir(temporaryDir)
}

tasks.withType(CppCompile::class).configureEach {
    compilerArgs.addAll(toolChain.map { toolChain ->
        if (this@configureEach.name.toLowerCase().contains("windows")) {
            when (toolChain) {
                is VisualCpp -> listOf("/std:c++11")
                is Gcc -> listOf(
                        "-lstdc++",
                        "-std=c++11",
                        "-static-libgcc",
                        "-static-libstdc++")
                is Clang -> listOf(
                        "-lstdc++",
                        "-std=c++11",
                        "-static-libstdc++")
                else -> listOf()
            }
        } else if (OperatingSystem.current().isMacOsX && this@configureEach.name.toLowerCase().contains("macos")) {
            when (toolChain) {
                is Gcc, is Clang -> listOf("-std=c++11")
                else -> listOf()
            }
        } else {
            when (toolChain) {
                is VisualCpp -> listOf("/std:c++11")
                is Gcc -> listOf(
                        "-lstdc++",
                        "-std=c++11",
                        "-static-libgcc",
                        "-static-libstdc++",
                        "-ldl")
                is Clang -> listOf(
                        "-lstdc++",
                        "-std=c++11",
                        "-static-libstdc++",
                        "-ldl")
                else -> listOf()
            }
        }
    })

    // A little bit of a weird workaround because we can't resolve targetMachine here
    if (name.toLowerCase().contains("android")) {
        compilerArgs.add("-llog")
    }
}

tasks.withType(LinkSharedLibrary::class).configureEach {
    linkerArgs.addAll(toolChain.map { toolChain ->
        val taskName = this@configureEach.name.toLowerCase()

        if (taskName.contains("windows")) {
            when (toolChain) {
                is Gcc -> listOf(
                        "-shared",
                        "-static-libgcc",
                        "-static-libstdc++")
                is Clang -> listOf(
                        "-shared",
                        "-static-libstdc++")
                else -> listOf()
            }
        } else if (taskName.contains("macos")) {
            if (OperatingSystem.current().isMacOsX) {
                when (toolChain) {
                    is Gcc, is Clang -> listOf(
                            "-shared",
                            "-Wl,-install_name,@rpath/libdevolay-natives.dylib")
                    else -> listOf()
                }
            } else {
                // Preserve the existing osxcross behavior on Linux.
                when (toolChain) {
                    is Gcc -> listOf(
                            "-shared",
                            "-static-libgcc",
                            "-static-libstdc++",
                            "-ldl",
                            "-Wl,-install_name,@rpath/libdevolay-natives.dylib")
                    is Clang -> listOf(
                            "-shared",
                            "-static-libstdc++",
                            "-ldl",
                            "-Wl,-install_name,@rpath/libdevolay-natives.dylib")
                    else -> listOf()
                }
            }
        } else {
            when (toolChain) {
                is Gcc -> listOf(
                        "-shared",
                        "-static-libgcc",
                        "-static-libstdc++",
                        "-ldl")
                is Clang -> listOf(
                        "-shared",
                        "-static-libstdc++",
                        "-ldl")
                else -> listOf()
            }
        }
    })

    // A little bit of a weird workaround because we can't resolve targetMachine here
    if (name.toLowerCase().contains("android")) {
        linkerArgs.add("-llog")
    }
}

library {
    targetMachines.set(listOf(
            machines.windows.x86, machines.windows.x86_64,
            machines.macOS.x86_64,
            machines.macOS.architecture("aarch64"),
            machines.linux.x86, machines.linux.x86_64,
            machines.os("android").architecture("armv7a"),
            machines.os("android").architecture("arm64-v8a"),
            machines.os("android").x86,
            machines.os("android").x86_64))

    // Include JNI headers generated by devolay-java
    val headerOnly: Configuration by configurations.creating {}
    dependencies {
        headerOnly(project(":devolay-java", "jniIncludes"))
    }

    privateHeaders {
        from(headerOnly)

        // Include platform-independent JNI Path
        from(downloadJniHeader)

        // Include our downloaded headers
        from(downloadNativeDependencies)

        // Include NDI headers
        from(files(locateNdiIncludes()))
    }

    binaries.whenElementFinalized {
        if (this.targetMachine.operatingSystemFamily.isWindows) {
            this.compileTask.get().dependsOn(downloadJniMdHeaderWindows)
            this.compileTask.get().includes.from(downloadJniMdHeaderWindows)
        } else {
            this.compileTask.get().dependsOn(downloadJniMdHeaderUnix)
            this.compileTask.get().includes.from(downloadJniMdHeaderUnix)
        }
    }
}

fun locateNdiIncludes(): Path {
    // Check system property
    var ndiSdk = if (System.getProperty("ndiSdk") != null) file(System.getProperty("ndiSdk")).toPath() else null

    // Check "NDI_SDK_DIR" environment variable
    if (ndiSdk == null && System.getenv("NDI_SDK_DIR") != null) {
        ndiSdk = file(System.getenv("NDI_SDK_DIR")).toPath()
    }

    // Use the vendored NDI headers when no explicit SDK override was provided.
    // These headers are individually licensed under MIT by Vizrt NDI AB.
    if (ndiSdk == null && file("src/main/ndi/include/Processing.NDI.Lib.h").exists()) {
        return file("src/main/ndi/include").toPath()
    }

    // Check typical install locations
    if (ndiSdk == null && OperatingSystem.current().isWindows && file("C:/Program Files/NDI 4 SDK").exists()) {
        ndiSdk = file("C:/Program Files/NDI 4 SDK").toPath()
    }
    if (ndiSdk == null && OperatingSystem.current().isMacOsX && file("/Library/NDI SDK for Apple").exists()) {
        ndiSdk = file("/Library/NDI SDK for Apple").toPath()
    }

    // Check the working directory
    if (ndiSdk == null && file("../NDI SDK for Linux").exists()) {
        ndiSdk = file("../NDI SDK for Linux").toPath()
    }
    if (ndiSdk == null && file("../NDI SDK for Apple").exists()) {
        ndiSdk = file("../NDI SDK for Apple").toPath()
    }
    if (ndiSdk == null && file("../NDI 4 SDK").exists()) {
        ndiSdk = file("NDI 4 SDK").toPath()
    }

    if (ndiSdk == null) {
        throw IllegalStateException("No NDI SDK found. Please set the NDI_SDK_DIR variable to the install location, run gradle with -DndiSdk=<Install Path>, or symlink the install path to your \"devolay\" folder.")
    }

    return when {
        Files.exists(ndiSdk.resolve("include")) -> {
            ndiSdk.resolve("include")
        }
        Files.exists(ndiSdk.resolve("Include")) -> {
            ndiSdk.resolve("Include")
        }
        else -> {
            throw IllegalStateException("NDI SDK at $ndiSdk is invalid: Has no 'include' or 'Include' subdirectory.")
        }
    }
}

// Resolve a complete NDI SDK for optional integrated packaging.
// Vendored MIT headers are intentionally not considered a complete SDK here.
fun locateIntegratedNdiSdkRoot(platform: String): Path? {
    val explicitProperty = System.getProperty("ndiSdk")
    if (!explicitProperty.isNullOrBlank()) {
        return file(explicitProperty).toPath()
    }

    val environmentPath = System.getenv("NDI_SDK_DIR")
    if (!environmentPath.isNullOrBlank()) {
        return file(environmentPath).toPath()
    }

    if (platform == "macos" &&
            file("/Library/NDI SDK for Apple").exists()) {
        return file("/Library/NDI SDK for Apple").toPath()
    }

    val checkoutCandidate = when (platform) {
        "windows" -> file("../NDI SDK for Windows").toPath()
        "macos" -> file("../NDI SDK for Apple").toPath()
        "linux" -> file("../NDI SDK for Linux").toPath()
        else -> null
    }

    return checkoutCandidate?.takeIf { Files.exists(it) }
}

fun requireIntegratedNdiFile(
        path: Path,
        description: String,
        platform: String,
        architecture: String): Path {
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
        throw GradleException(
                "Integrated NDI packaging requested, but $description was not found at " +
                "$path for $platform/$architecture.")
    }
    return path
}

// Integrated NDI runtime packaging is explicitly opt-in.
val enableIntegratedNdi =
        providers.gradleProperty("enableIntegratedNdi")
                .map { it.toBoolean() }
                .orElse(false)

// Public CI must not package proprietary NDI runtime binaries accidentally.
// Controlled/private CI may override this guard explicitly.
val allowIntegratedNdiInCi =
        providers.gradleProperty("allowIntegratedNdiInCi")
                .map { it.toBoolean() }
                .orElse(false)

val runningInCi =
        System.getenv("CI")?.equals("true", ignoreCase = true) == true

if (enableIntegratedNdi.get() &&
        runningInCi &&
        !allowIntegratedNdiInCi.get()) {
    throw GradleException(
            "Integrated NDI packaging is disabled in CI by default. " +
            "Use -PallowIntegratedNdiInCi=true only in a controlled environment.")
}

// Add artifacts for devolay-java to depend on
val assembleNativeArtifacts by tasks.registering(Jar::class) {
    archiveBaseName.set("devolay-native-artifacts")
    destinationDirectory.set(temporaryDir)

    components.withType(ComponentWithBinaries::class).forEach { component ->
        (component as ComponentWithBinaries).binaries.whenElementFinalized(ComponentWithOutputs::class.java) {
            if (this is ComponentWithNativeRuntime) {
                val machine = this.targetMachine

                // Only include release binaries
                if (this.isOptimized && !this.getName().toLowerCase().contains("debug") && machine.operatingSystemFamily.name != "android") {
                    from(this.outputs) {
                        into("natives/" + machine.operatingSystemFamily.name + "/" + machine.architecture.name)
                        exclude("*.lib")
                        exclude("*.debug")
                        exclude("*.dwarf")
                    }
                }
            }
        }
    }
}

val assembleIntegratedNDIArtifacts by tasks.registering(Jar::class) {
    archiveBaseName.set("ndi-lib-artifacts")
    destinationDirectory.set(temporaryDir)
    enabled = enableIntegratedNdi.get()

    if (enableIntegratedNdi.get()) {
        components.withType(ComponentWithBinaries::class).forEach { component ->
            (component as ComponentWithBinaries).binaries.whenElementFinalized(ComponentWithOutputs::class.java) {
                if (this is ComponentWithNativeRuntime && this.isOptimized) {
                    val machine = this.targetMachine
                    val platform = machine.operatingSystemFamily.name
                    val architecture = machine.architecture.name

                    // Android has its own packaging path below.
                    if (platform == "android") {
                        return@whenElementFinalized
                    }

                    // Modern integrated desktop support is intentionally 64-bit.
                    // Legacy 32-bit native targets remain available to the normal
                    // Devolay build, but are not packaged with an NDI runtime.
                    val supportedIntegratedTarget =
                            (platform == "windows" &&
                                    architecture == "x86-64") ||
                            (platform == "macos" &&
                                    (architecture == "x86-64" ||
                                            architecture == "aarch64")) ||
                            (platform == "linux" &&
                                    architecture == "x86-64")

                    if (!supportedIntegratedTarget) {
                        return@whenElementFinalized
                    }

                    val ndiSdkRoot =
                            locateIntegratedNdiSdkRoot(platform)
                                    ?: throw GradleException(
                                            "Integrated NDI packaging requested for " +
                                                    "$platform/$architecture, but no complete " +
                                                    "NDI SDK was found. Set -DndiSdk=<path> or " +
                                                    "NDI_SDK_DIR=<path>.")

                    val (nativeLibName, nativeLibPath, nativeLicensePath) =
                            when (platform) {
                                "windows" -> {
                                    val runtime =
                                            requireIntegratedNdiFile(
                                                    ndiSdkRoot.resolve(
                                                            "Bin/x64/Processing.NDI.Lib.x64.dll"),
                                                    "NDI runtime",
                                                    platform,
                                                    architecture)

                                    val license =
                                            requireIntegratedNdiFile(
                                                    ndiSdkRoot.resolve(
                                                            "Bin/x64/Processing.NDI.Lib.Licenses.txt"),
                                                    "NDI runtime license file",
                                                    platform,
                                                    architecture)

                                    Triple("ndi.dll", runtime, license)
                                }

                                "macos" -> {
                                    // Current NDI SDK for Apple ships a universal
                                    // libndi.dylib for Intel and Apple Silicon.
                                    val runtime =
                                            requireIntegratedNdiFile(
                                                    ndiSdkRoot.resolve(
                                                            "lib/macOS/libndi.dylib"),
                                                    "NDI runtime",
                                                    platform,
                                                    architecture)

                                    val license =
                                            requireIntegratedNdiFile(
                                                    ndiSdkRoot.resolve(
                                                            "licenses/libndi_licenses.txt"),
                                                    "NDI runtime license file",
                                                    platform,
                                                    architecture)

                                    Triple("libndi.dylib", runtime, license)
                                }

                                "linux" -> {
                                    val runtimeDirectory =
                                            ndiSdkRoot.resolve(
                                                    "lib/x86_64-linux-gnu")

                                    if (!Files.exists(runtimeDirectory) ||
                                            !Files.isDirectory(runtimeDirectory)) {
                                        throw GradleException(
                                                "Integrated NDI packaging requested, but " +
                                                        "the NDI runtime directory was not found at " +
                                                        "$runtimeDirectory for " +
                                                        "$platform/$architecture.")
                                    }

                                    // Linux SDK distributions may provide libndi.so
                                    // symlinks plus a versioned regular binary.
                                    // Package the actual binary, not a symlink.
                                    val runtime =
                                            Files.newDirectoryStream(
                                                    runtimeDirectory).use { entries ->
                                                entries.asSequence()
                                                        .firstOrNull {
                                                            Files.isRegularFile(it) &&
                                                                    !Files.isSymbolicLink(it) &&
                                                                    it.fileName.toString()
                                                                            .startsWith("libndi.so") &&
                                                                    Files.size(it) > 10 * 1000
                                                        }
                                            } ?: throw GradleException(
                                                    "Integrated NDI packaging requested, but " +
                                                            "no regular libndi.so runtime binary " +
                                                            "was found in $runtimeDirectory for " +
                                                            "$platform/$architecture.")

                                    val license =
                                            requireIntegratedNdiFile(
                                                    ndiSdkRoot.resolve(
                                                            "licenses/libndi_licenses.txt"),
                                                    "NDI runtime license file",
                                                    platform,
                                                    architecture)

                                    Triple("libndi.so", runtime, license)
                                }

                                else -> return@whenElementFinalized
                            }

                    println(
                            "Adding NDI lib from $nativeLibPath " +
                                    "to integrated build.")

                    from(nativeLibPath) {
                        rename {
                            nativeLibName
                        }
                        into(
                                "natives/" +
                                        platform +
                                        "/" +
                                        architecture)
                    }

                    from(nativeLicensePath) {
                        into(
                                "natives/" +
                                        platform +
                                        "/" +
                                        architecture)
                    }
                }
            }
        }
    }
}

val assembleAndroidArtifacts by tasks.registering(Copy::class) {
    into(temporaryDir)

    components.withType(ComponentWithBinaries::class).forEach { component ->
        (component as ComponentWithBinaries).binaries.whenElementFinalized(ComponentWithOutputs::class.java) {
            if (this is ComponentWithNativeRuntime && this.isOptimized) {
                val machine = this.targetMachine

                var nativeLibPath: Path? = null;
                var androidAbi: String? = null;

                if (machine.operatingSystemFamily.name == "android") {
                    // Identify android architecture
                    when (machine.architecture.name) {
                        "armv7a" -> {
                            nativeLibPath = file("../NDI SDK for Android/lib/armeabi-v7a/libndi.so").toPath()
                            androidAbi = "armeabi-v7a"
                        }
                        "arm64-v8a" -> {
                            nativeLibPath = file("../NDI SDK for Android/lib/arm64-v8a/libndi.so").toPath()
                            androidAbi = "arm64-v8a"
                        }
                        "x86" -> {
                            nativeLibPath = file("../NDI SDK for Android/lib/x86/libndi.so").toPath()
                            androidAbi = "x86"
                        }
                        "x86-64" -> {
                            nativeLibPath = file("../NDI SDK for Android/lib/x86_64/libndi.so").toPath()
                            androidAbi = "x86_64"
                        }
                    }

                    // Add NDI Binaries
                    if (nativeLibPath == null || !Files.exists(nativeLibPath)) {
                        System.err.println("Could not find NDI lib in expected location (" + nativeLibPath.toString() + ") for OS \"" + machine.operatingSystemFamily.name + "\" and arch \"" + machine.architecture.name + "\". No android builds available.")
                    } else {
                        println("Adding NDI lib from " + nativeLibPath.toString() + " to android build.")
                        from(nativeLibPath!!) {
                            into("jni/" + androidAbi!!)
                        }
                        from("../NDI SDK for Android/licenses/Bonjour.txt") {
                            into("jni/" + androidAbi!!)
                        }
                        from("../NDI SDK for Android/licenses/libndi_licenses.txt") {
                            into("jni/" + androidAbi!!)
                        }
                    }

                    // Add Devolay binaries
                    from(this.outputs) {
                        into("jni/" + androidAbi!!)
                        exclude("*.lib")
                        exclude("*.debug")
                    }
                }
            }
        }
    }
}

val nativeArtifacts: Configuration? by configurations.creating
val integratedNdiArtifacts: Configuration? by configurations.creating
val androidArtifacts: Configuration? by configurations.creating

artifacts {
    add("nativeArtifacts", assembleNativeArtifacts)
    add("integratedNdiArtifacts", assembleIntegratedNDIArtifacts)
    add("androidArtifacts", assembleAndroidArtifacts)
}
