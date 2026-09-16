# Android build and installation

The Android module is a separate Gradle project so it can use Android Gradle
Plugin 8.7.3, Java 17, and the Android SDK without destabilising the upstream
desktop build. It compiles the shared engine sources with Android-specific
renderer, audio, storage, image-codec, and platform backends.

## Status and target

Version 0.9.0 is a playable ARM64 test build validated on an Anbernic RG405V.
It requires:

- Android 8.0/API 26 or newer;
- a 64-bit ARM (`arm64-v8a`) device;
- OpenGL ES 3.2 support;
- a physical controller for normal play.

Other Android devices are not yet validated. There are no touchscreen gameplay
controls.

## Build

Install Java 17, Android SDK platform 35, Android build-tools 35.0.0, and Gradle
8.9 or newer. From this directory run:

```text
gradle --no-daemon :app:assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs the same build and uploads `severed-chains-android-debug`
as a workflow artifact. Debug APKs use a debug signing identity. A public
release needs a deliberately managed release-signing key; no private signing
material belongs in this repository.

## Install and first start

Install the APK with Android's package installer or ADB. On first start, select
all four legally obtained Legend of Dragoon disc images together. The app copies
them into its private storage, runs the normal Severed Chains extraction and
patching pipeline, and then starts the game.

Imported discs, extracted files, configuration, and saves live in app-private
storage. Installing an update over the existing package preserves that data;
uninstalling the app removes it. The app and repository do not include disc
images or extracted game data.

## Architecture

- `app/` contains the Android activity, GLES renderer, input bridge, packaged
  runtime assets, and app resources.
- `engine/` contains Android implementations of shared engine services and the
  documented Java 17 compatibility artifacts required by the game.
- `../src/main/java` remains the shared engine source tree used by desktop and
  Android builds.
- `ANDROID_PORT_LOG.md` records the porting decisions and device-specific fixes.

The Android port deliberately avoids copying the whole upstream engine into a
second source tree. Platform services are injected where desktop-only APIs such
as SDL, LWJGL, OpenAL, STB, Discord SDK, and JavaFX are unavailable on Android.
