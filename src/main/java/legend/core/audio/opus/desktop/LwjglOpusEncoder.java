package legend.core.audio.opus.desktop;

import legend.core.DirectBuffers;
import legend.core.audio.opus.OpusEncoder;
import org.lwjgl.util.opus.Opus;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public final class LwjglOpusEncoder implements OpusEncoder {
  private final long encoder;
  private final ByteBuffer output;

  public LwjglOpusEncoder(final int channels) {
    this.encoder = Opus.opus_encoder_create(48_000, channels, Opus.OPUS_APPLICATION_AUDIO, null);
    Opus.opus_encoder_ctl(this.encoder, Opus.OPUS_SET_BITRATE(128_000));
    this.output = DirectBuffers.bytes(500 * channels);
  }

  @Override public void reset() { Opus.opus_encoder_ctl(this.encoder, Opus.OPUS_RESET_STATE); }
  @Override public byte[] encode(final ShortBuffer pcm, final int frameSize) {
    this.output.clear();
    final int count = Opus.opus_encode(this.encoder, pcm, frameSize, this.output);
    final byte[] bytes = new byte[count];
    this.output.get(bytes, 0, count);
    return bytes;
  }
  @Override public void destroy() { Opus.opus_encoder_destroy(this.encoder); }
}
