package legend.core.audio;

import java.util.Objects;
import java.util.function.Supplier;

public final class AudioBackends {
  private static Supplier<AudioBackend> factory = AudioBackends::desktopBackend;
  private static AudioBackend current;

  private AudioBackends() { }

  public static synchronized void install(final Supplier<AudioBackend> factory) {
    AudioBackends.factory = Objects.requireNonNull(factory);
  }

  public static synchronized AudioBackend create() {
    current = factory.get();
    return current;
  }

  public static synchronized AudioBackend current() {
    if(current == null) throw new IllegalStateException("Audio backend has not been initialized");
    return current;
  }

  private static AudioBackend desktopBackend() {
    try {
      return (AudioBackend)Class.forName("legend.core.audio.desktop.OpenAlBackend").getConstructor().newInstance();
    } catch(final ReflectiveOperationException | LinkageError e) {
      throw new IllegalStateException("No audio backend is available", e);
    }
  }
}
