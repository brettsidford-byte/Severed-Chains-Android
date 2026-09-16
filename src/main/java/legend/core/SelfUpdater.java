package legend.core;

import legend.core.lang.I18nText;
import legend.core.lang.TextComponent;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

/** Platform-neutral entry point for optional host self-update support. */
public final class SelfUpdater {
  private SelfUpdater() { }

  public enum UpdateState { LAUNCHING_UPDATER, DONE, FAILED }
  public record UpdateProgress(UpdateState state, TextComponent message) { }

  public static boolean launchUpdater(final String downloadUrl, final Consumer<UpdateProgress> progressCallback) {
    try {
      return (boolean)Class.forName("legend.core.desktop.DesktopSelfUpdater")
        .getMethod("launch", String.class, Consumer.class)
        .invoke(null, downloadUrl, progressCallback);
    } catch(final ClassNotFoundException e) {
      progressCallback.accept(new UpdateProgress(UpdateState.FAILED, new I18nText("lod_core.ui.updater.unsupported")));
      return false;
    } catch(final ReflectiveOperationException | LinkageError e) {
      final Throwable cause = e instanceof InvocationTargetException invocation && invocation.getCause() != null
        ? invocation.getCause() : e;
      throw new RuntimeException("Failed to launch platform updater", cause);
    }
  }
}
