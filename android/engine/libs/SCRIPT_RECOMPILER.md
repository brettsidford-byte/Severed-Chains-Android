# Script Recompiler Android artifact

`script-recompiler-0.7.11-java17.jar` is a Java 17-compatible build of
[`Legend-of-Dragoon-Modding/Script-Recompiler`](https://github.com/Legend-of-Dragoon-Modding/Script-Recompiler)
version 0.7.11. The upstream project and this repository are licensed under the
GNU Affero General Public License v3.0.

The source archive used was GitHub's `main` archive with SHA-256
`A07F1704F4B038AD578B372A9BBB0E1D1360F69DF2385AF924EE3D22AB33D7F2`.
It was compiled with Gradle 8.9, JDK 21, and `--release 17`. To preserve the
same implementation on Android, six source-level conveniences unavailable in
Java 17 were replaced with their equivalent operations:

- `List.getFirst()` -> `List.get(0)` (three occurrences)
- `List.getLast()` -> `List.get(size() - 1)`
- `List.removeLast()` -> `List.remove(size() - 1)`
- `StringBuilder.repeat(value, count)` -> `StringBuilder.append(value.repeat(count))`
- Logger initialization in runtime-used classes names its class explicitly because
  Log4j caller inference is unavailable on Android Runtime.
- Runtime include loading uses `Files.readAllBytes` plus UTF-8 decoding because
  the RG405V ART library does not provide `Files.readString`.

The resulting jar has SHA-256
`6C1FF1AD1DE455889B8CA44EECAA42D8A342A3363089946E4796B2919DE4F4E8`.
