package legend.core.audio.opus;

import java.nio.ShortBuffer;

public interface OpusEncoder {
  void reset();
  byte[] encode(ShortBuffer pcm, int frameSize);
  void destroy();
}
