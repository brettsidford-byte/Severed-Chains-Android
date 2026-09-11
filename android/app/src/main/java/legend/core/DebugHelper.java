package legend.core;

public final class DebugHelper {
  private DebugHelper() { }
  public static void sleep(final long millis) {
    try { Thread.sleep(millis); } catch(final InterruptedException e) { Thread.currentThread().interrupt(); }
  }
}
