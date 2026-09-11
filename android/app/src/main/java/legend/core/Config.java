package legend.core;

import java.io.IOException;

public final class Config {
  private Config() { }

  // The Android unpacker must use file-backed input from the first pass.
  // The desktop in-memory strategy can exceed the RG405V app heap.
  private static volatile boolean lowMemory = true;

  public static boolean lowMemoryUnpacker() {
    return lowMemory;
  }

  public static void enableLowMemoryUnpacker() {
    lowMemory = true;
  }

  public static void save() throws IOException {
    // Android extraction state is held by the app and does not require
    // the desktop configuration writer.
  }
}
