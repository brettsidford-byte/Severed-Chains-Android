package legend.core.platform;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Selects the platform implementation without coupling the engine lifecycle to
 * one windowing library. Desktop keeps SDL as the default; Android can install
 * its implementation before GameEngine is initialised.
 */
public final class PlatformManagerFactory {
  private static Supplier<PlatformManager> factory = PlatformManagerFactory::createDesktop;

  private PlatformManagerFactory() {
  }

  public static void setFactory(final Supplier<PlatformManager> factory) {
    PlatformManagerFactory.factory = Objects.requireNonNull(factory);
  }

  public static PlatformManager create() {
    return factory.get();
  }

  private static PlatformManager createDesktop() {
    try {
      return (PlatformManager)Class.forName("legend.core.platform.SdlPlatformManager")
        .getConstructor().newInstance();
    } catch(final ReflectiveOperationException e) {
      throw new IllegalStateException("Desktop platform manager is unavailable", e);
    }
  }
}
