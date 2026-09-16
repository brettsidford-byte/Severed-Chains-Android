package legend.core.audio.opus;

import java.util.Objects;
import java.util.function.IntFunction;

public final class OpusEncoders {
  private static IntFunction<OpusEncoder> factory = OpusEncoders::desktopEncoder;
  private OpusEncoders() { }
  public static void install(final IntFunction<OpusEncoder> factory) { OpusEncoders.factory = Objects.requireNonNull(factory); }
  public static OpusEncoder create(final int channels) { return factory.apply(channels); }
  private static OpusEncoder desktopEncoder(final int channels) {
    try {
      return (OpusEncoder)Class.forName("legend.core.audio.opus.desktop.LwjglOpusEncoder")
        .getConstructor(int.class).newInstance(channels);
    } catch(final ReflectiveOperationException | LinkageError e) {
      throw new IllegalStateException("No Opus encoder is available", e);
    }
  }
}
