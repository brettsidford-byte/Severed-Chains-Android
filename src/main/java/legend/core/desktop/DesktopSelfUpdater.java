package legend.core.desktop;

import legend.core.SelfUpdater;
import legend.core.Version;
import legend.core.lang.I18nText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

public final class DesktopSelfUpdater {
  private static final Logger LOGGER = LogManager.getFormatterLogger(DesktopSelfUpdater.class);
  private DesktopSelfUpdater() { }

  public static boolean launch(final String downloadUrl, final Consumer<SelfUpdater.UpdateProgress> progressCallback) {
    try {
      final Path gameDir = Path.of(".").toAbsolutePath().normalize();
      final Path updaterJar = gameDir.resolve("updater.jar");
      if(!Files.exists(updaterJar)) {
        progressCallback.accept(failed("lod_core.ui.updater.updater_not_found"));
        return false;
      }
      final Path javaExe = findJavaExecutable(gameDir);
      if(javaExe == null) {
        progressCallback.accept(failed("lod_core.ui.updater.java_not_found"));
        return false;
      }
      final long pid = ProcessHandle.current().pid();
      progressCallback.accept(new SelfUpdater.UpdateProgress(SelfUpdater.UpdateState.LAUNCHING_UPDATER, new I18nText("lod_core.ui.updater.launching_updater")));
      final ProcessBuilder process = new ProcessBuilder(javaExe.toString(), "-jar", updaterJar.toString(), downloadUrl,
        gameDir.toString(), String.valueOf(pid), Version.FULL_VERSION);
      process.directory(gameDir.toFile());
      process.inheritIO();
      process.start();
      progressCallback.accept(new SelfUpdater.UpdateProgress(SelfUpdater.UpdateState.DONE, new I18nText("lod_core.ui.updater.updater_launched")));
      return true;
    } catch(final IOException e) {
      LOGGER.error("Failed to launch updater", e);
      progressCallback.accept(failed("lod_core.ui.updater.updater_launch_failed"));
      return false;
    }
  }

  private static SelfUpdater.UpdateProgress failed(final String key) {
    return new SelfUpdater.UpdateProgress(SelfUpdater.UpdateState.FAILED, new I18nText(key));
  }

  private static Path findJavaExecutable(final Path gameDir) {
    final var command = ProcessHandle.current().info().command();
    if(command.isPresent() && Files.exists(Path.of(command.get()))) return Path.of(command.get());
    final String exeName = System.getProperty("os.name", "").toLowerCase(Locale.US).contains("win") ? "java.exe" : "java";
    final Path bundled = gameDir.resolve("jdk25").resolve("bin").resolve(exeName);
    if(Files.exists(bundled)) return bundled;
    try(final var stream = Files.newDirectoryStream(gameDir, "jdk25*")) {
      for(final Path dir : stream) {
        final Path candidate = dir.resolve("bin").resolve(exeName);
        if(Files.exists(candidate)) return candidate;
      }
    } catch(final IOException ignored) { }
    final Path systemJava = Path.of(System.getProperty("java.home")).resolve("bin").resolve(exeName);
    return Files.exists(systemJava) ? systemJava : null;
  }
}
