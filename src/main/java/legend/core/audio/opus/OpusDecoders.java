package legend.core.audio.opus;

import java.util.Objects;
import java.util.function.Supplier;

public final class OpusDecoders {
  private static Supplier<OpusDecoder> factory = OpusDecoders::desktopDecoder;
  private OpusDecoders() { }
  public static void install(final Supplier<OpusDecoder> factory) { OpusDecoders.factory = Objects.requireNonNull(factory); }
  public static OpusDecoder create() { return factory.get(); }
  private static OpusDecoder desktopDecoder() {
    try {
      return (OpusDecoder)Class.forName("legend.core.audio.opus.desktop.LwjglOpusDecoder").getConstructor().newInstance();
    } catch(final ReflectiveOperationException | LinkageError e) {
      throw new IllegalStateException("No Opus decoder is available", e);
    }
  }
}
