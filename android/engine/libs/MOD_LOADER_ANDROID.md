# Mod Loader Android compatibility build

`mod-loader-4.3.3-android.jar` starts from the exact published Mod Loader 4.3.3
binary and retains its desktop discovery API. Two source-compatible entry points
were added from the published 4.3.3 source artifact:

- `ModManager.Access.findBundledMods` registers known in-APK mod classes without
  desktop classpath scanning.
- `EventManager.Access.initializeBundled` registers known in-APK event listeners
  without trying to enumerate classes from Android DEX files.
- `LangManager` uses the Java 17/Android-compatible `Locale` constructor in place
  of the newer `Locale.of` host-JDK call.
- Bundled discovery tolerates Android Runtime classes without a protection domain
  or code source, which are desktop-JVM concepts and may be absent on ART.
- Bundled locale discovery likewise skips desktop JAR enumeration on ART while
  retaining normal class-loader resource lookup and English fallback behavior.
- Event validation checks the Java modifiers directly for bundled static listeners,
  avoiding `Method.canAccess`, which is absent from the RG405V ART library.

This is required because Reflections/URLClassLoader discovery cannot enumerate
classes stored in an Android APK. The compatibility JAR SHA-256 is
`CD3FD21E399173BBDCAECEC32ABF03AC0E3CAEC08814424DEA311F7F5645C9A9`.
