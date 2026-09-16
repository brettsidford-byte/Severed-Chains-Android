package legend.core.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat.Builder;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Android PCM output backed by one streaming AudioTrack per engine source. */
public final class AndroidAudioBackend implements AudioBackend {
  @Override public List<String> devices() { return List.of("<default>"); }
  @Override public boolean init(final String requestedDevice) { return true; }
  @Override public boolean isConnected() { return true; }
  @Override public boolean defaultDeviceChanged() { return false; }
  @Override public AudioSink createSink(final int bufferCount) { return new AndroidAudioSink(bufferCount); }
  @Override public void destroy() { }

  private static final class AndroidAudioSink implements AudioSink {
    private AudioTrack track;
    private int format = -1;
    private int sampleRate;
    private final ThreadPoolExecutor writer;

    private AndroidAudioSink(final int bufferCount) {
      this.writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(Math.max(2, bufferCount)), runnable -> {
          final Thread thread = new Thread(runnable, "severed-chains-audio-sink");
          thread.setDaemon(true);
          return thread;
        });
    }

    @Override public boolean canBuffer() { return !this.writer.isShutdown() && this.writer.getQueue().remainingCapacity() > 0; }
    @Override public void processBuffers() { }

    private void ensureTrack(final int format, final int sampleRate) {
      if(this.track != null && this.format == format && this.sampleRate == sampleRate) return;
      this.releaseTrack();
      final int channelMask = format == AudioFormat.MONO_8 || format == AudioFormat.MONO_16
        ? android.media.AudioFormat.CHANNEL_OUT_MONO : android.media.AudioFormat.CHANNEL_OUT_STEREO;
      final int encoding = format == AudioFormat.MONO_8
        ? android.media.AudioFormat.ENCODING_PCM_8BIT
        : format == AudioFormat.STEREO_FLOAT
          ? android.media.AudioFormat.ENCODING_PCM_FLOAT
          : android.media.AudioFormat.ENCODING_PCM_16BIT;
      final int minimum = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding);
      if(minimum <= 0) throw new IllegalStateException("Unsupported Android PCM format " + format);
      this.track = new AudioTrack.Builder()
        .setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build())
        .setAudioFormat(new Builder().setSampleRate(sampleRate).setChannelMask(channelMask).setEncoding(encoding).build())
        .setBufferSizeInBytes(Math.max(minimum, sampleRate / 10 * (channelMask == android.media.AudioFormat.CHANNEL_OUT_MONO ? 1 : 2) * 4))
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build();
      this.format = format;
      this.sampleRate = sampleRate;
    }

    private void start() {
      if(this.track.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) this.track.play();
    }

    @Override
    public void queue(final int format, final ByteBuffer data, final int sampleRate) {
      this.ensureTrack(format, sampleRate);
      this.start();
      final byte[] copy = new byte[data.remaining()];
      data.duplicate().get(copy);
      this.writer.execute(() -> {
        final AudioTrack current = this.track;
        if(current != null) current.write(copy, 0, copy.length, AudioTrack.WRITE_BLOCKING);
      });
    }

    @Override
    public void queue(final int format, final short[] data, final int sampleRate) {
      this.ensureTrack(format, sampleRate);
      this.start();
      final short[] copy = Arrays.copyOf(data, data.length);
      this.writer.execute(() -> {
        final AudioTrack current = this.track;
        if(current != null) current.write(copy, 0, copy.length, AudioTrack.WRITE_BLOCKING);
      });
    }

    @Override
    public void queue(final int format, final float[] data, final int sampleRate) {
      this.ensureTrack(format, sampleRate);
      this.start();
      final float[] copy = Arrays.copyOf(data, data.length);
      this.writer.execute(() -> {
        final AudioTrack current = this.track;
        if(current != null) current.write(copy, 0, copy.length, AudioTrack.WRITE_BLOCKING);
      });
    }

    @Override public void play() { if(this.track != null) this.start(); }
    @Override public void stop() { if(this.track != null && this.track.getState() == AudioTrack.STATE_INITIALIZED) { this.track.pause(); this.track.flush(); } }
    @Override public float position() { return this.track == null || this.sampleRate == 0 ? 0.0f : this.track.getPlaybackHeadPosition() / (float)this.sampleRate; }

    @Override
    public void destroy() {
      this.writer.shutdownNow();
      this.releaseTrack();
    }

    private void releaseTrack() {
      if(this.track != null) {
        try { this.track.pause(); this.track.flush(); } catch(final IllegalStateException ignored) { }
        try { this.track.stop(); } catch(final IllegalStateException ignored) { }
        this.track.release();
        this.track = null;
      }
    }
  }
}
