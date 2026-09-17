# Android build and installation

The Android module is a separate Gradle project so it can use Android Gradle
Plugin 8.7.3, Java 17, and the Android SDK without destabilising the upstream
desktop build. It compiles the shared engine sources with Android-specific
renderer, audio, storage, image-codec, and platform backends.

## Status and target

Version 0.9.3 is a playable ARM64 release build validated on an Anbernic RG405V.
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
release uses the manually triggered `Android signed release APK` workflow.

## Signed release

Generate the permanent release key locally. Do not commit it or send it through
chat or email:

```text
keytool -genkeypair -v -keystore severed-chains-release.jks -alias severed-chains -keyalg RSA -keysize 4096 -validity 10000
```

Keep secure offline backups of the keystore, alias, and passwords. Every update
to the release application must be signed by this same key.

Convert the keystore to a single-line Base64 value in PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("severed-chains-release.jks")) | Set-Clipboard
```

Create these repository secrets under **Settings > Secrets and variables >
Actions**:

- `ANDROID_KEYSTORE_BASE64`: the Base64 value copied above;
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password;
- `ANDROID_KEY_ALIAS`: `severed-chains` (or the alias chosen at creation);
- `ANDROID_KEY_PASSWORD`: the private-key password.

Run **Actions > Android signed release APK > Run workflow** on the
`android-port` branch. The workflow reconstructs the keystore only inside the
temporary runner, builds `:app:assembleRelease`, verifies the signature, and
uploads `severed-chains-android-release`. The keystore directory is ignored by
Git and the temporary runner copy is removed even if the build fails.

Release builds use package `legend.severedchains.android`; debug builds use
`legend.severedchains.android.debug`. Android treats them as separate apps, so
the first release installation does not automatically inherit debug saves or
imported disc data.

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
