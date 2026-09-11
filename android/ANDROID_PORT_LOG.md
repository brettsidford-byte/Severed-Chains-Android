# Android port development log

## 2026-09-11 — storage and diagnostics milestone

- Confirmed the multi-file import APK builds successfully in GitHub Actions and installs on the RG405V.
- Added a central Android storage-path object for game data, extracted files, saves, patches, mods, and configuration.
- Added persistent Android logcat diagnostics for imports, OpenGL ES initialisation, and physical controller events.
- Improved document naming by using Android's display name where available.
- Added an import summary showing filenames and total size.
- This build still contains the GLES surface probe; the desktop SDL/LWJGL engine is not yet connected.
