# Android port development log

## 2026-09-11 — storage and diagnostics milestone

- Confirmed the multi-file import APK builds successfully in GitHub Actions and installs on the RG405V.
- Added a central Android storage-path object for game data, extracted files, saves, patches, mods, and configuration.
- Added persistent Android logcat diagnostics for imports, OpenGL ES initialisation, and physical controller events.
- Improved document naming by using Android's display name where available.
- Added an import summary showing filenames and total size.
- This build still contains the GLES surface probe; the desktop SDL/LWJGL engine is not yet connected.

## 2026-09-11 — shared storage-root seam

- Added `legend.core.GamePaths` with a desktop working-directory default and configurable Android root.
- Updated config, engine asset/save paths, and the upstream unpacker to use that root.
- Android now sets `severed.chains.root` to its private app directory before future engine startup.
- The Android APK still does not launch the engine; SDL/LWJGL/JavaFX dependencies remain to be isolated.

## 2026-09-11 — shared class included in Android source set

- Included the real upstream `legend.core.GamePaths` source in the Android module instead of maintaining an Android duplicate.
- Android configures that shared class directly at activity startup, so future engine components resolve `isos`, `files`, `saves`, `patches`, `mods`, and configuration under the app's private storage root.
- Bumped the Android debug build to version `0.3.0`.
- Build/device result: awaiting the new GitHub Actions build; the RG405V has not yet been retested with this revision.


## 2026-09-11 — Android platform seam and controller state

- Added `PlatformManagerFactory` and changed `GameEngine` to obtain its platform manager through the factory; desktop continues to default to SDL.
- Added an Android-native controller state adapter for D-pad, face buttons, shoulders, triggers, start/select, and both analogue sticks.
- Routed handheld gamepad key events through the activity and retained GLES surface diagnostics.
- The Android module remains intentionally source-limited: the real engine still requires the desktop SDL/LWJGL/JavaFX graph and an Android GLES renderer backend.
- Build/device result: awaiting Actions validation; no RG405V test has been performed for this revision.
