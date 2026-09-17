plugins {
    id("com.android.application")
}

val sharedShaderAssetsDir = layout.buildDirectory.dir("generated/sharedShaderAssets")
val expectedGlesShaders = fileTree("src/main/gles-shaders") {
    include("*.vsh", "*.gsh", "*.fsh")
}

val verifyGlesShaders = tasks.register("verifyGlesShaders") {
    inputs.files(expectedGlesShaders)
    doLast {
        val files = expectedGlesShaders.files.sortedBy { it.name }
        check(files.size == 13) { "Expected 13 precompiled GLES shaders, found ${files.size}" }
        files.forEach { shader ->
            check(shader.readText().trimStart().startsWith("#version 320 es")) {
                "${shader.name} is not precompiled GLSL ES 3.20"
            }
        }
    }
}

val copySharedShaderAssets = tasks.register("copySharedShaderAssets") {
    dependsOn(verifyGlesShaders)
    doLast {
        copy {
            from("../../gfx")
            into(sharedShaderAssetsDir.get().asFile.resolve("runtime/gfx"))
        }
        copy {
            from("src/main/gles-shaders")
            into(sharedShaderAssetsDir.get().asFile.resolve("gfx/shaders"))
        }
        copy {
            from("../../lang")
            into(sharedShaderAssetsDir.get().asFile.resolve("runtime/lang"))
        }
        copy {
            from("../../patches")
            into(sharedShaderAssetsDir.get().asFile.resolve("runtime/patches"))
        }
    }
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
val releaseSigningValues = mapOf(
    "ANDROID_KEYSTORE_PATH" to releaseKeystorePath,
    "ANDROID_KEYSTORE_PASSWORD" to releaseKeystorePassword,
    "ANDROID_KEY_ALIAS" to releaseKeyAlias,
    "ANDROID_KEY_PASSWORD" to releaseKeyPassword,
)
val missingReleaseSigningValues = releaseSigningValues
    .filterValues { it.isNullOrBlank() }
    .keys

if (releaseBuildRequested && missingReleaseSigningValues.isNotEmpty()) {
    throw GradleException(
        "Release signing is not configured. Set: ${missingReleaseSigningValues.joinToString()}",
    )
}
val releaseSigningConfigured = missingReleaseSigningValues.isEmpty()

android {
    namespace = "legend.severedchains.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "legend.severedchains.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.9.2"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isDebuggable = false
            // Keep the first signed build behaviourally identical to the tested
            // debug build. Reflection-heavy engine/mod code needs explicit R8
            // rules before shrinking can be enabled safely.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    packaging {
        jniLibs {
            // The RG405V package manager cannot mmap the preset FFmpeg payload
            // directly from this debug APK; extract it into the app's native dir.
            useLegacyPackaging = true
            // JavaCV uses the JNI libraries, not the preset command-line tools;
            // non-lib filenames under lib/<abi> are rejected by some installers.
            excludes += setOf("**/ffmpeg", "**/ffprobe")
        }
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/legend/core/updater.fxml"
            excludes += "/legend/game/debugger/**"
            excludes += "/lib/**/ffmpeg"
            excludes += "/lib/**/ffprobe"
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(sharedShaderAssetsDir)
            java.srcDirs("../../src/main/java", "../engine/src/main/java")
            resources.srcDirs("../../src/main/resources")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    exclude("legend/core/platform/Sdl*.java")
    exclude("legend/core/platform/input/Sdl*.java")
    exclude("legend/core/renderer/opengl/**")
    exclude("legend/core/renderer/opengles/**")
    exclude("discord/desktop/**")
    exclude("legend/core/gpu/desktop/**")
    exclude("legend/core/audio/desktop/**")
    exclude("legend/core/audio/opus/desktop/**")
    exclude("legend/game/textures/desktop/**")
    exclude("legend/game/unpacker/midi/SoundbankDecoder.java")
    exclude("legend/game/Main.java")
    exclude("legend/game/MainWindows.java")
    exclude("legend/core/desktop/**")
    exclude("legend/game/debugger/**")
    exclude("legend/core/UpdaterApplication.java")
    exclude("legend/core/UpdaterController.java")
    exclude("legend/core/UpdaterMain.java")
}

dependencies {
    implementation(files("../engine/libs/mod-loader-4.3.3-android.jar"))
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.slf4j:slf4j-nop:2.0.7")
    implementation(files("../engine/libs/script-recompiler-0.7.11-java17.jar"))
    implementation("org.antlr:antlr4:4.13.2")
    implementation("org.fusesource.jansi:jansi:2.4.3")
    implementation("commons-cli:commons-cli:1.6.0")
    implementation(files("../engine/libs/mogul-dabas-0.0.1-java17.jar"))
    implementation("org.apache.logging.log4j:log4j-api:2.26.1")
    implementation("org.apache.logging.log4j:log4j-core:2.26.1")
    implementation("org.joml:joml:1.10.8")
    implementation("org.json:json:20250107")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.opencsv:opencsv:5.9")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("commons-io:commons-io:2.18.0")
    implementation("com.github.slugify:slugify:3.0.7")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.bytedeco:javacv:1.5.13")
    implementation("org.bytedeco:ffmpeg:8.0-1.5.13:android-arm64")
}

tasks.named("preBuild") {
    dependsOn(copySharedShaderAssets)
}
