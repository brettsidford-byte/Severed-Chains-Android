plugins {
    id("com.android.application")
}

val sharedGamePathsDir = layout.buildDirectory.dir("generated/sharedGamePaths")
val sharedIsoReaderDir = layout.buildDirectory.dir("generated/sharedIsoReader")
val sharedFileDataDir = layout.buildDirectory.dir("generated/sharedFileData")
val sharedFileDataSupportDir = layout.buildDirectory.dir("generated/sharedFileDataSupport")
val sharedUnpackerStructureDir = layout.buildDirectory.dir("generated/sharedUnpackerStructure")
val sharedUnpackerOrchestrationDir = layout.buildDirectory.dir("generated/sharedUnpackerOrchestration")
val sharedGlesBackendDir = layout.buildDirectory.dir("generated/sharedGlesBackend")

val copySharedGamePaths = tasks.register("copySharedGamePaths") {
    doLast {
        copy {
            from("../../src/main/java/legend/core/GamePaths.java")
            into(sharedGamePathsDir.get().asFile.resolve("legend/core"))
        }
    }
}

val copySharedIsoReader = tasks.register("copySharedIsoReader") {
    doLast {
        copy {
            from("../../src/main/java/legend/game/unpacker/IsoReader.java")
            into(sharedIsoReaderDir.get().asFile.resolve("legend/game/unpacker"))
        }
    }
}

val copySharedFileData = tasks.register("copySharedFileData") {
    doLast {
        copy {
            from(
                "../../src/main/java/legend/game/unpacker/FileData.java",
                "../../src/main/java/legend/game/unpacker/FileBackedFileData.java",
                "../../src/main/java/legend/game/unpacker/ExpandableFileData.java",
                "../../src/main/java/legend/game/unpacker/MrgArchive.java",
                "../../src/main/java/legend/game/unpacker/DeffArchive.java"
            )
            into(sharedFileDataDir.get().asFile.resolve("legend/game/unpacker"))
        }
    }
}

val copySharedFileDataSupport = tasks.register("copySharedFileDataSupport") {
    doLast {
        copy {
            from("../../src/main/java/legend/core/gpu/Rect4i.java")
            into(sharedFileDataSupportDir.get().asFile.resolve("legend/core/gpu"))
        }
        copy {
            from("../../src/main/java/legend/core/gte/MV.java")
            into(sharedFileDataSupportDir.get().asFile.resolve("legend/core/gte"))
        }
        copy {
            from("../../src/main/java/legend/core/memory/types/IntRef.java")
            into(sharedFileDataSupportDir.get().asFile.resolve("legend/core/memory/types"))
        }
    }
}

val copySharedUnpackerStructure = tasks.register("copySharedUnpackerStructure") {
    doLast {
        copy {
            from(
                "../../src/main/java/legend/game/unpacker/DirectoryEntry.java",
                "../../src/main/java/legend/game/unpacker/FileMap.java",
                "../../src/main/java/legend/game/unpacker/PathNode.java",
                "../../src/main/java/legend/game/unpacker/UnpackerException.java",
                "../../src/main/java/legend/game/unpacker/UnpackerStoppedRuntimeException.java"
            )
            into(sharedUnpackerStructureDir.get().asFile.resolve("legend/game/unpacker"))
        }
    }
}

val copySharedUnpackerOrchestration = tasks.register("copySharedUnpackerOrchestration") {
    doLast {
        copy {
            from(
                "../../src/main/java/legend/game/unpacker/Transformations.java",
                "../../src/main/java/legend/game/unpacker/LeafTransformation.java",
                "../../src/main/java/legend/game/unpacker/BranchTransformation.java",
                "../../src/main/java/legend/game/unpacker/Replacement.java",
                "../../src/main/java/legend/game/unpacker/Unpacker.java"
            )
            into(sharedUnpackerOrchestrationDir.get().asFile.resolve("legend/game/unpacker"))
        }
    }
}


val copySharedGlesBackend = tasks.register("copySharedGlesBackend") {
    doLast {
        copy {
            from(
                "../../src/main/java/legend/core/renderer/opengles/GlesApi.java",
                "../../src/main/java/legend/core/renderer/opengles/GlesFrameBuffer.java",
                "../../src/main/java/legend/core/renderer/opengles/GlesMesh.java",
                "../../src/main/java/legend/core/renderer/opengles/GlesShader.java",
                "../../src/main/java/legend/core/renderer/opengles/GlesShaderUniformBuffer.java",
                "../../src/main/java/legend/core/renderer/opengles/GlesTexture.java"
            )
            into(sharedGlesBackendDir.get().asFile.resolve("legend/core/renderer/opengles"))
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
        versionCode = 7
        versionName = "0.7.0-unpacker-orchestration"

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

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/java",
                sharedGamePathsDir,
                sharedIsoReaderDir,
                sharedFileDataDir,
                sharedFileDataSupportDir,
                sharedUnpackerStructureDir,
                sharedUnpackerOrchestrationDir,
                sharedGlesBackendDir
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.joml:joml:1.10.8")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.apache.logging.log4j:log4j-api:2.26.1")
    implementation("org.apache.logging.log4j:log4j-core:2.26.1")
}

tasks.named("preBuild") {
    dependsOn(copySharedGamePaths)
    dependsOn(copySharedIsoReader)
    dependsOn(copySharedFileData)
    dependsOn(copySharedFileDataSupport)
    dependsOn(copySharedUnpackerStructure)
    dependsOn(copySharedUnpackerOrchestration)
    dependsOn(copySharedGlesBackend)
}
