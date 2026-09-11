package legend.core;

import java.io.IOException;

public final class Config {
  private Config() { }
  public static boolean lowMemoryUnpacker() { return false; }
  public static void enableLowMemoryUnpacker() { }
  public static void save() throws IOException { }
}
