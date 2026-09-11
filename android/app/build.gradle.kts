plugins {
    id("com.android.application")
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
            java.srcDirs("../../src/main/java")
            java.include("legend/severedchains/android/**")
            java.include("legend/core/GamePaths.java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
