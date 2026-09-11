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

## 2026-09-11 — renderer capability probe

- Added an Android GL-thread probe for GLSL ES 3 shaders with uniform blocks, framebuffer objects, and integer textures used by the upstream renderer.
- The probe records GLES version, renderer, extensions, maximum texture size, and uniform-block capacity in logcat.
- This is a compatibility validation layer; it does not alter the existing render resolution or claim that the game renderer is connected.
- Build result: GitHub Actions runs 58, 59, and final run 61 succeeded. The final revision also corrects framebuffer diagnostic status reporting.
- Device result: APK is not yet installed/tested on the RG405V for this revision.

## 2026-09-11 — upstream-compatible ISO preflight

- Replaced Android's loose first-32-MiB string scan with the same PlayStation sector-16 volume-header validation used by the upstream unpacker.
- Android now validates 2352-byte sector images, `CD001`, `PLAYSTATION`, and the exact four Severed Chains volume IDs.
- No duplicate extraction or transformation code was added.
- Build result: GitHub Actions runs 63 and 64 succeeded.
- The full upstream unpacker remains to be included with its dependency graph before extraction can execute on Android.

## 2026-09-11 — upstream IsoReader integration boundary

- Attempted to include the complete upstream unpacker package; compilation exposed dependencies on core GPU/math, game, audio, logging, and third-party classes.
- Reverted that broad inclusion to preserve a green APK.
- Included the actual upstream `IsoReader` class in the Android source set and routed Android disc preflight through it.
- Build result: GitHub Actions run 70 succeeded.
- Remaining extraction work is to port/include the unpacker's dependency graph, not to write a replacement extractor.

## 2026-09-11 — file-data/archive dependency group

- Included the actual upstream `FileData`, `FileBackedFileData`, `ExpandableFileData`, `MrgArchive`, and `DeffArchive` classes.
- Added the upstream `Rect4i`, `MV`, and `IntRef` support classes.
- Added a small Android-only Java 17 binary helper with the byte-array operations required by upstream file data.
- Added Java 17-compatible Android registry types instead of the upstream Java 21 mod-loader binary.
- Added Android packaging rules for duplicate dependency metadata.
- Build result: GitHub Actions run 77 succeeded and produced the ARM64 debug APK.
- The APK has not yet been retested on the RG405V for this revision. The real unpacker orchestration, engine startup, renderer backend, audio backend, and normal introduction screen remain future stages.

## 2026-09-11 — unpacker structural dependency group

- Included actual upstream `DirectoryEntry`, `FileMap`, `PathNode`, `UnpackerException`, and `UnpackerStoppedRuntimeException` classes.
- These classes provide the ISO directory tree, file-map output, transformation path tree, and existing error types needed by the real unpacker.
- Build result: GitHub Actions run 80 succeeded and produced a new ARM64 debug APK.
- The Android app still performs ISO recognition only; the complete upstream `Unpacker` remains blocked on its transformation, XA-audio, script, and game-segment dependencies.

- 2026-09-11 — Added the upstream Unpacker orchestration boundary and Java-17 Android compatibility shims for status, tuple, I/O, configuration, and executor/file APIs. The first compile exposed and fixed a duplicate executor scope. CI run 92 then failed during dependency resolution with broad transient Maven lookup errors before Java compilation; retry run 93 succeeded. The extraction action was then connected to the Android screen, and run 94 produced an ARM64 APK. Optional portrait/CTMD/submap/audio transformer hooks remain isolated Android boundaries and are not yet full implementations.
- 2026-09-11 — Added “Run existing Severed Chains extraction” to MainActivity. When all four recognised discs are present it calls the upstream Unpacker on the Android GamePaths root and forwards progress to the screen. Build/test: run 94 succeeded; no RG405V execution test performed in this environment.
- 2026-09-11 — Added AndroidEngineRenderBridge and routed GLSurfaceView creation, resize, and frame callbacks through it. Build/test: run 96 succeeded. This is the renderer lifecycle seam only; the existing desktop RenderEngine still depends on LWJGL/JavaFX and is not yet running on Android.
- 2026-09-11 — Added AndroidGlesRenderBackend proof: ES 3 shaders, indexed vertex/index buffers, vertex attributes, blending and GL-thread drawing; connected to AndroidEngineRenderBridge. Build/test: latest combined run succeeded. This validates GLES primitives only; it is not yet the Severed Chains RenderApi.


## 2026-09-11 — upstream GLES backend dependency audit

- Attempted to compile the actual upstream `legend.core.renderer.opengles` classes as the next renderer pass.
- The source uses LWJGL OpenGLES/OpenGL bindings and the complete shared `RenderApi`/shader/uniform contract graph; Android's `android.opengl.GLES20/GLES30` APIs cannot satisfy those imports directly.
- CI run 109 failed during Java compilation with missing `org.lwjgl.opengles` and renderer-contract classes.
- Removed the incomplete source boundary to restore the previously working Android build.
- The next renderer pass must add an Android-native RenderApi adapter or a deliberately scoped LWJGL-to-GLES compatibility layer; the upstream classes are retained as reference, not claimed as Android-ready.


## 2026-09-11 — Android renderer contract boundary

- Added `AndroidRenderApi` as the Android-side rendering lifecycle contract.
- Routed the existing GLES proof backend and engine render bridge through that contract.
- Kept the current GLES 3 proof output unchanged while removing direct backend ownership from the bridge.
- Build/test: CI validation pending; this remains a renderer-platform step and does not yet connect the full Severed Chains `RenderApi`.


## 2026-09-11 — GLES shader and texture resource boundary

- Added Android GLES resource helpers for shader-program compilation/linking and RGBA texture upload, filtering, wrapping, and deletion.
- Refactored the proof renderer to use the shared Android shader-program path.
- Texture upload is available for the next texture-backed draw pass; no game asset is bundled or altered.
- Build/test: CI validation pending.


## 2026-09-11 — GLES framebuffer resource boundary

- Added an Android GLES framebuffer object with RGBA colour texture attachment and optional depth/stencil renderbuffer.
- Added framebuffer completeness validation and lifecycle cleanup.
- The GLES proof backend now creates and validates a 1×1 off-screen target on the GL thread without changing the visible proof output.
- Build/test: CI validation pending; physical-device framebuffer behaviour remains to be checked on the RG405V.


## 2026-09-11 — GLES mesh and indexed-buffer boundary

- Added an Android GLES mesh resource owning VAO, vertex buffer, index buffer, attribute layout, indexed drawing, and cleanup.
- Moved the proof renderer's vertex/index submission out of the bridge/backend lifecycle and into the mesh resource.
- The current proof still uses a small coloured quad; no game geometry or asset format has been changed.
- Build/test: CI validation pending.


## 2026-09-11 — texture-backed mesh boundary

- Added UV-coordinate support to the Android GLES mesh resource.
- Updated the proof shader and mesh to sample an uploaded RGBA texture while retaining vertex colour modulation.
- This validates the texture–shader–mesh path without bundling game assets or changing the target resolution.
- Build/test: CI validation pending; the proof remains the only visible renderer.


## 2026-09-11 — upstream shader asset boundary

- Added the repository's real `gfx/shaders` files to the Android asset pipeline.
- Added an Android shader-source loader that changes `#version 330 core` to `#version 300 es` while preserving the shader body.
- Added a GL-thread probe compiling the upstream `simple.vsh` and `simple.fsh` pair after adaptation.
- Geometry-shader assets remain excluded from this initial probe because the target GLES 3.0 context does not expose the desktop geometry stage.
- Build/test: CI validation pending; actual shader execution still requires the full Severed Chains uniform and mesh setup.


## 2026-09-11 — standard shader binding boundary

- Added Android uniform-block binding helpers.
- Added an Android binding description for the real upstream `standard.vsh`/`standard.fsh` pair.
- Verified the expected seven vertex attributes: position, normal, UV, texture page, CLUT, colour, and flags.
- Added bindings for the real `transforms`, `transforms2`, `lighting`, `clutAnimation`, `projectionInfo`, and `scissor` uniform blocks.
- The GL-thread diagnostic now reports whether this actual shader contract can be created on Android.
- Build/test: CI validation pending; no claim is made yet about rendering a real game scene.
