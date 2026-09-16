plugins {
    id("com.android.library")
}

android {
    namespace = "legend.severedchains.engine"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("../../src/main/java", "src/main/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    // Host implementations are replaced by Android app-layer services.
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

    // Desktop-only tooling is not part of the game runtime on Android.
    exclude("legend/game/debugger/**")
    exclude("legend/core/UpdaterApplication.java")
    exclude("legend/core/UpdaterController.java")
    exclude("legend/core/UpdaterMain.java")
}

dependencies {
    api(files("libs/mod-loader-4.3.3-android.jar"))
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.slf4j:slf4j-nop:2.0.7")
    api(files("libs/script-recompiler-0.7.11-java17.jar"))
    implementation("org.antlr:antlr4:4.13.2")
    implementation("org.fusesource.jansi:jansi:2.4.3")
    implementation("commons-cli:commons-cli:1.6.0")
    api(files("libs/mogul-dabas-0.0.1-java17.jar"))
    api("org.apache.logging.log4j:log4j-api:2.26.1")
    api("org.apache.logging.log4j:log4j-core:2.26.1")
    api("org.joml:joml:1.10.8")
    implementation("org.json:json:20250107")
    api("com.google.code.gson:gson:2.13.2")
    implementation("com.opencsv:opencsv:5.9")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("commons-io:commons-io:2.18.0")
    api("com.github.slugify:slugify:3.0.7")
    api("it.unimi.dsi:fastutil:8.5.15")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.bytedeco:javacv:1.5.13")
    implementation("org.bytedeco:ffmpeg:8.0-1.5.13:android-arm64")
}
