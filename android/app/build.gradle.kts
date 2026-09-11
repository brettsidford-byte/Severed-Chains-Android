plugins {
    id("com.android.application")
}

val sharedGamePathsDir = layout.buildDirectory.dir("generated/sharedGamePaths")
val copySharedGamePaths = tasks.register("copySharedGamePaths") {
    doLast {
        copy {
            from("../../src/main/java/legend/core/GamePaths.java")
            into(sharedGamePathsDir.get().asFile.resolve("legend/core"))
        }
    }
}

android {
    namespace = "legend.severedchains.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "legend.severedchains.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-shared-paths"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", sharedGamePathsDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild") {
    dependsOn(copySharedGamePaths)
}
