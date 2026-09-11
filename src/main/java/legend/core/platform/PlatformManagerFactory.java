package legend.core.platform;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Selects the platform implementation without coupling the engine lifecycle to
 * one windowing library. Desktop keeps SDL as the default; Android can install
 * its implementation before GameEngine is initialised.
 */
public final class PlatformManagerFactory {
  private static Supplier<PlatformManager> factory = SdlPlatformManager::new;

  private PlatformManagerFactory() {
  }

  public static void setFactory(final Supplier<PlatformManager> factory) {
    PlatformManagerFactory.factory = Objects.requireNonNull(factory);
  }

  public static PlatformManager create() {
    return factory.get();
  }
}
