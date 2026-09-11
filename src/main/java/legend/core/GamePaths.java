package legend.core;

import java.nio.file.Path;

public final class GamePaths {
  private static final String ROOT_PROPERTY = "severed.chains.root";
  private static volatile Path root = initialRoot();

  private GamePaths() { }

  public static Path root() {
    return root;
  }

  public static void configure(final Path newRoot) {
    root = newRoot.toAbsolutePath().normalize();
  }

  public static Path resolve(final String first, final String... more) {
    Path path = root.resolve(first);
    for(final String part : more) {
      path = path.resolve(part);
    }
    return path;
  }

  public static Path configFile() {
    return resolve("config.conf");
  }

  public static Path configDcnf() {
    return resolve("config.dcnf");
  }

  public static Path files() {
    return resolve("files");
  }

  public static Path isos() {
    return resolve("isos");
  }

  public static Path saves() {
    return resolve("saves");
  }

  public static Path patches() {
    return resolve("patches");
  }

  public static Path mods() {
    return resolve("mods");
  }

  public static Path gfx() {
    return resolve("gfx");
  }

  public static Path lang() {
    return resolve("lang");
  }

  private static Path initialRoot() {
    return Path.of(System.getProperty(ROOT_PROPERTY, ".")).toAbsolutePath().normalize();
  }
}
