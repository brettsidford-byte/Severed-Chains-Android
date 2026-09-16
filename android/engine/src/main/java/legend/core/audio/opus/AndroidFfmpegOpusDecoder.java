package legend.core.audio.opus;

import legend.game.unpacker.FileData;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.io.ByteArrayInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

/** Android ARM64 Opus decoder backed by the bundled FFmpeg native libraries. */
public final class AndroidFfmpegOpusDecoder implements OpusDecoder {
  private short[] samples = new short[0];
  private int channels;
  private int cursor;

  @Override
  public void open(final FileData data) {
    this.close();
    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new ByteArrayInputStream(data.getBytes()))) {
      grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
      grabber.start();
      this.channels = Math.max(1, grabber.getAudioChannels());
      int size = 0;
      short[] decoded = new short[48_000 * this.channels];
      Frame frame;
      while((frame = grabber.grabSamples()) != null) {
        if(frame.samples == null || frame.samples.length == 0) continue;
        final int frameSamples = frame.samples[0].remaining();
        final int needed = Math.multiplyExact(frameSamples, this.channels);
        if(size + needed > decoded.length) decoded = Arrays.copyOf(decoded, Math.max(size + needed, decoded.length * 2));
        if(frame.samples.length == 1) {
          final ShortBuffer source = asShortBuffer(frame.samples[0]);
          final int count = Math.min(needed, source.remaining());
          source.get(decoded, size, count);
          size += count;
        } else {
          final ShortBuffer[] planes = new ShortBuffer[this.channels];
          for(int channel = 0; channel < this.channels; channel++) planes[channel] = asShortBuffer(frame.samples[channel]);
          for(int sample = 0; sample < frameSamples; sample++) {
            for(int channel = 0; channel < this.channels; channel++) decoded[size++] = planes[channel].get();
          }
        }
      }
      grabber.stop();
      this.samples = Arrays.copyOf(decoded, size);
    } catch(final Exception exception) {
      throw new IllegalArgumentException("Error opening Android Opus XA file", exception);
    }
  }

  private static ShortBuffer asShortBuffer(final Buffer source) {
    if(source instanceof ShortBuffer shorts) return shorts.duplicate();
    if(source instanceof ByteBuffer bytes) return bytes.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer();
    throw new IllegalArgumentException("Unsupported FFmpeg PCM buffer " + source.getClass().getName());
  }

  @Override public int channels() { return this.channels; }
  @Override public long sampleCount() { return this.samples.length; }
  @Override public int read(final short[] pcm) {
    final int count = Math.min(pcm.length, this.samples.length - this.cursor);
    if(count <= 0) return 0;
    System.arraycopy(this.samples, this.cursor, pcm, 0, count);
    this.cursor += count;
    return count;
  }
  @Override public void close() { this.samples = new short[0]; this.channels = 0; this.cursor = 0; }
}
