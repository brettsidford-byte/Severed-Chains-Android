package legend.core.audio.opus;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/** Synchronous 48 kHz Opus packet encoder used by the Android XA transcoder. */
public final class AndroidMediaCodecOpusEncoder implements OpusEncoder {
  private static final long TIMEOUT_US = 100_000;
  private final MediaCodec codec;
  private final int channels;
  private long presentationUs;

  public AndroidMediaCodecOpusEncoder(final int channels) {
    this.channels = channels;
    try {
      this.codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS);
      final MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48_000, channels);
      format.setInteger(MediaFormat.KEY_BIT_RATE, 128_000);
      format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 5760 * channels * Short.BYTES);
      this.codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      this.codec.start();
    } catch(final IOException | RuntimeException exception) {
      throw new IllegalStateException("Android device has no usable Opus encoder", exception);
    }
  }

  @Override
  public void reset() {
    this.codec.flush();
    this.presentationUs = 0;
  }

  @Override
  public byte[] encode(final ShortBuffer pcm, final int frameSize) {
    final int inputIndex = this.codec.dequeueInputBuffer(TIMEOUT_US);
    if(inputIndex < 0) throw new IllegalStateException("Timed out waiting for Android Opus input buffer");
    final ByteBuffer input = this.codec.getInputBuffer(inputIndex);
    if(input == null) throw new IllegalStateException("Android Opus input buffer is unavailable");
    input.clear();
    input.order(ByteOrder.nativeOrder()).asShortBuffer().put(pcm.duplicate());
    final int byteCount = frameSize * this.channels * Short.BYTES;
    this.codec.queueInputBuffer(inputIndex, 0, byteCount, this.presentationUs, 0);
    this.presentationUs += frameSize * 1_000_000L / 48_000L;

    final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    for(int attempts = 0; attempts < 8; attempts++) {
      final int outputIndex = this.codec.dequeueOutputBuffer(info, TIMEOUT_US);
      if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) continue;
      if(outputIndex >= 0) {
        final ByteBuffer output = this.codec.getOutputBuffer(outputIndex);
        if(output == null) throw new IllegalStateException("Android Opus output buffer is unavailable");
        final byte[] packet = new byte[info.size];
        output.position(info.offset).limit(info.offset + info.size);
        output.get(packet);
        this.codec.releaseOutputBuffer(outputIndex, false);
        if((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && packet.length != 0) return packet;
      }
    }
    throw new IllegalStateException("Timed out waiting for an Android Opus packet");
  }

  @Override
  public void destroy() {
    try { this.codec.stop(); } finally { this.codec.release(); }
  }
}
