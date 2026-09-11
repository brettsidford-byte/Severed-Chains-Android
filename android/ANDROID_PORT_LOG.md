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


## 2026-09-11 — standard shader uniform-buffer boundary

- Added Android GLES uniform-buffer objects with std140-sized storage, upload, binding and cleanup.
- The standard-shader GL probe now allocates and binds the six real Severed Chains uniform blocks:
  `transforms`, `transforms2`, `lighting`, `clutAnimation`, `projectionInfo`, and `scissor`.
- This uploads safe zero-filled defaults for binding validation only; it does not yet render a game mesh.
- Build/test: CI validation pending.


## 2026-09-11 — packed standard uniform data

- Replaced zero-filled standard-shader validation buffers with packed std140 defaults.
- Added identity camera, projection and model transforms.
- Added a valid default lighting record, CLUT-animation terminator, projection parameters, and a 640×480 scissor rectangle.
- The GL-thread probe now uploads and binds this structured data before cleanup.
- These are renderer-layout defaults only; game-state camera, lighting and model values are not yet connected.
- Build/test: CI validation pending.


## 2026-09-11 — standard-shader mesh submission boundary

- Added the actual 16-float Severed Chains vertex layout to the Android mesh resource.
- The GL-thread probe now submits indexed mesh data with position, normal, UV, texture-page, CLUT, colour and flags attributes.
- Set the standard shader's live scalar/vector uniforms and draw state.
- This is the first Android submission through the real Severed Chains standard shader; it remains a controlled probe and is not yet fed by extracted game geometry.
- Build/test: CI validation pending.


## 2026-09-11 — extracted TMD vertex bridge

- Added a minimal Android real-data probe that locates the first extracted `.tmd` file under `GamePaths.files()`.
- Reads its actual PS1 vertex table and converts three extracted vertices into the upstream 16-float standard-shader layout.
- Submits that extracted geometry through the real standard shader and packed uniform buffers.
- This is intentionally a narrow proof boundary; full primitive decoding, CLUT texture upload, lighting and scene traversal remain to be connected.
- Build/test: CI validation pending; physical-device execution is still required to confirm the extracted tree contains discoverable TMD files.


## 2026-09-11 — first extracted TMD primitive decode

- Extended the real-data bridge to read the first TMD primitive group header.
- Applied the upstream packet-size rules for triangle/quad, lit, shaded and textured primitive variants.
- Decoded the first primitive's actual vertex indices and submitted the corresponding extracted geometry.
- Textured packets are identified but currently rendered through the untextured safety path; CLUT and VRAM texture upload remain next.
- Build/test: CI validation pending.


## 2026-09-11 — TMD texture metadata and PS1 conversion

- Decoded the first textured primitive's actual UV coordinates, CLUT value and tpage value.
- Added a PS1 BGR555-to-RGBA texture conversion helper for Android GLES uploads.
- The extracted primitive still uses the untextured safety path because the unpacked VRAM source has not yet been connected to the tpage/CLUT coordinates.
- Build/test: CI validation pending.


## 2026-09-11 — Android PS1 VRAM boundary

- Added an Android-native 1024×512 PS1 VRAM store matching the upstream GPU's 15-bit upload model.
- Added rectangle uploads, 15-bit reads, RGBA region conversion, and page/CLUT texel addressing for 4bpp, 8bpp and direct 15-bit textures.
- Kept the VRAM source data in PS1 format and expand only the region intended for GLES upload.
- This is the data boundary needed before a real textured TMD can be drawn; it is not yet connected to the live game GPU command stream.
- Build/test: CI validation pending.


## 2026-09-11 — rebind unpacker output on Android

- Fixed a path-initialisation issue where the upstream unpacker's static output root could be captured before Android configured `GamePaths`.
- The unpacker now rebinds its output and replacement paths at the start of each extraction.
- This ensures the completion marker and extracted files are written under the same Android app-private root used by the readiness check.
- Build/test: CI validation pending; device retest required.


## 2026-09-11 — retry low-memory extraction on Android

- Confirmed the upstream unpacker can catch Android memory pressure, enable its low-memory mode, and return without writing the completion marker.
- Android now checks the marker after extraction and automatically retries once using the enabled low-memory path.
- Corrected extraction status messages to use real line breaks and to distinguish completed extraction from an incomplete pass.
- Build/test: CI validation pending; device retest required.


## 2026-09-11 — extraction retry build identification

- Bumped the Android version to 0.8.0 and added a visible build label to the status screen.
- This makes it possible to confirm that the replacement APK has actually updated the installed application before testing extraction.
- Build/test: CI validation pending.


## 2026-09-11 — enable Android low-memory unpacker mode

- Found the cause of the repeated incomplete extraction: the Android Config shim always returned `false` for `lowMemoryUnpacker()` and ignored the enable call.
- Changed the Android shim to use file-backed low-memory extraction from the first pass and to retain the enabled state.
- This avoids the desktop in-memory strategy that exceeds the RG405V application heap.
- Build/test: CI validation pending; device retest required.


## 2026-09-11 — Android engine lifecycle host

- Added an Android-owned engine host that coordinates extracted data, GLES-surface readiness and render-thread startup state.
- Connected the host to the GLSurfaceView lifecycle and start it automatically after successful extraction.
- Kept the desktop `GameEngine.start()` untouched; its LWJGL/JavaFX window loop is not yet callable from Android.
- The APK now reaches an explicit Android engine-host/render-bridge phase, preparing the next pass for real engine frame submission.
- Build/test: CI validation pending; this is a lifecycle milestone, not yet the normal game introduction.


## 2026-09-11 — persistent Android game-frame path

- Added an Android-owned monotonic frame clock with bounded delta time.
- The engine host now starts and ticks this frame loop from the GLSurfaceView GL callback.
- This replaces the desktop window-loop assumption at the lifecycle boundary and provides the frame timing/input point for the future game-state update.
- Extracted geometry still uses the diagnostic submission path; the normal Severed Chains engine/render graph is not yet connected.
- Build/test: CI validation pending.


## 2026-09-11 — stream ISO members in Android low-memory mode

- ADB confirmed a 192 MiB Android heap limit and repeated ~37 MiB allocation failures during extraction.
- Found that the previous low-memory path still called `IsoReader.readSectors()`, allocating each complete ISO member before writing it to disk.
- Added sector-by-sector streaming directly to temporary files and returned `FileBackedFileData` over those files.
- This keeps the extraction working set near one 2352-byte sector plus transformer buffers instead of a whole member byte array.
- Build/test: CI validation pending; RG405V retest required.

## 2026-09-11 — Android GLES shader compatibility

- Added Android-only explicit fragment float precision and float-uniform comparison literals when adapting upstream GLSL to GLES 3.0.
- Standard shader readiness now requires both expected attribute locations and all std140 uniform blocks.
- Desktop GLSL sources remain unchanged.

## 2026-09-11 — Android GLES texture resource layer

- Added an Android render-thread texture resource covering RGB8, RGBA8, R32UI, and depth formats used by the upstream renderer.
- Added bounded sub-image updates, texture-unit binding, filtering/wrap configuration, and idempotent deletion.
- This is an adapter component; the temporary diagnostic backend remains active until the complete RenderApi port is assembled.

## 2026-09-11 — Android GLES mesh index/update support

- Added a standard upstream vertex-layout path using 32-bit GLES element buffers, matching the desktop renderer's `int[]` index contract.
- Added complete vertex-stream updates for dynamic and streaming mesh usage; existing 16-bit diagnostic mesh paths remain unchanged.

2026-09-11: Added Android mesh primitive-mode and indexed-range draw operations with bounds checks, preparing the adapter for upstream line, strip, and partial-batch rendering.

2026-09-11: Added a reusable Android GLES shader-program resource with upstream-style asset loading, uniform setters, uniform-block binding, and idempotent cleanup. Existing diagnostic rendering remains unchanged until the complete RenderApi adapter is assembled.

## 2026-09-11 — Android GLES framebuffer binding safety

- The framebuffer adapter now restores the previously bound framebuffer after creation instead of forcing the default target.
- Added an explicit complete-target bind path for the future Android RenderApi backend.


## 2026-09-11 — atomic ISO import and normal-flow cleanup

- Changed Android ISO copying to write each selected document to a temporary `.part` file, reject empty results, and move it into the final `GamePaths.isos()` directory only after the copy completes.
- Changed the import manifest to use the same temporary-then-replace pattern, preventing a partial manifest from claiming incomplete files are ready.
- Removed the separate extraction action from the normal Android interface. The selected ISO files are now treated as the game-data input for Severed Chains startup, matching the desktop program's expected `isos` directory flow.
- Updated the status screen to show the final ISO directory and clarify that engine startup integration is still pending.
- Build/test: CI validation pending; the previous RG405V build remains the last physical-device result.


## 2026-09-11 — automatic Android startup hand-off

- Changed the Android activity to request engine startup automatically when four ISO files are already present, including immediately after a successful import.
- The Android engine host now invokes the existing upstream `Unpacker` internally on a worker thread using the configured `GamePaths.isos()` directory.
- Startup progress and failures are written to logcat; no separate extraction control is exposed to the user.
- This pass reaches the existing upstream data-preparation boundary, but the full `GameEngine` render loop is still not connected to the Android GLES backend.
- Build/test: CI validation pending; physical RG405V test required.


## 2026-09-11 — bound Android unpacker worker count

- RG405V logcat showed `pool-4-thread-1647` and `pool-4-thread-2689` while the unpacker was still transforming files.
- The upstream cached executor was therefore creating thousands of workers; this is unsuitable for Android's small heap and shared-file access.
- The low-memory unpacker path now uses a fixed two-worker pool for transformations and file writes. Desktop cached-pool behaviour is preserved.
- Made the file-backed data handle registry safe while worker threads are opening and closing temporary ISO members.
- Build/test: CI validation pending; the currently running device process must be stopped before retesting.

## 2026-09-11 — validate ISO readiness by upstream disc identity

- Changed Android startup gating to require all four upstream Severed Chains PlayStation volume IDs, rather than counting manifest entries or arbitrary files in `isos/`.
- Kept the Android-private `GamePaths.isos()` location and atomic import behavior unchanged.
- This confirms the Android input seam matches the upstream program's `isos/` contract; the real `GameEngine.start()` platform integration remains a separate stage.
- Build/test: source change made; CI/device validation pending.

## 2026-09-11 — initialise canonical Android storage layout

- Android now creates the canonical root directories at startup before configuring shared `GamePaths`: `isos/`, `files/`, `saves/`, `patches/`, and `mods/`.
- Startup logging now reports the exact ISO and extracted-files paths, making any import/path mismatch directly verifiable through ADB.
- No existing files are deleted or moved.
- Build/test: CI validation pending; RG405V retest required.

## 2026-09-11 — stream large file-backed outputs

- The RG405V crashed while writing a 109 MB extracted asset because `FileBackedFileData.write()` materialised the entire file through a direct buffer.
- File-backed output now streams through a bounded 1 MB buffer, preserving the existing file-backed source and desktop behavior while avoiding large transient allocations.
- Build/test: CI validation pending; RG405V retest required.

## 2026-09-11 — Android unpacker transformation diagnostics

- Added low-memory-path diagnostics around each upstream leaf transformation, including worker path, selected transformer, elapsed time, and remaining queue count.
- Desktop cached-pool behavior and logging remain unchanged; Android-only diagnostics are enabled only when low-memory unpacking is active.
- This is intended to identify the exact file or transformer behind the RG405V transformation stall.
- Build/test: source change made; CI/device validation pending.

## 2026-09-11 — tolerate absent Chester replacement targets

- The RG405V reached the upstream Chester texture replacement with a missing `260/textures/4` node and crashed with a `NullPointerException`.
- The replacement now checks each archive path before replacing it and logs a skip when the target is absent; normal replacement behaviour is unchanged when the target exists.
- Build/test: CI validation pending; RG405V retest required.
