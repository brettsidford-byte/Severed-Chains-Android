# Android port

The Android module is a separate Gradle project so it can use an Android
toolchain without destabilising the desktop Java 25 build.

## Current stage

The module currently builds an ARM64 debug APK containing an Android lifecycle
and fullscreen packaging probe. It is not yet the game and must not be
described as a playable port. The probe exists to validate GitHub Actions,
Android packaging, landscape/fullscreen behaviour and installation on the
RG405V before integrating the shared engine.

Build locally from this directory with:

    gradle :app:assembleDebug

The APK output is:

    app/build/outputs/apk/debug/app-debug.apk

The next stage adds the shared platform/storage seams and replaces this probe
Activity with the Android renderer and engine host. No game data is included.
