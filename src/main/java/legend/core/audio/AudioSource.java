package legend.core.audio;

import java.nio.ByteBuffer;

public abstract class AudioSource {
  private final int bufferCount;
  private AudioSink sink;
  private boolean active;

  public AudioSource(final int bufferCount) {
    this.bufferCount = bufferCount;
  }

  protected boolean isInitialized() {
    return this.sink != null;
  }

  protected void init() {
    this.sink = AudioBackends.current().createSink(this.bufferCount);
  }

  protected void destroy() {
    this.active = false;
    if(this.sink != null) {
      this.sink.destroy();
      this.sink = null;
    }
  }

  public void tick() {
    if(this.isActive()) this.play();
  }

  public boolean canBuffer() {
    return this.active && this.sink != null && this.sink.canBuffer();
  }

  protected void handleProcessedBuffers() {
    if(this.sink != null) this.sink.processBuffers();
  }

  protected void bufferOutput(final int format, final ByteBuffer buffer, final int sampleRate) {
    synchronized(this) {
      if(this.sink != null && this.sink.canBuffer()) this.sink.queue(format, buffer, sampleRate);
    }
  }

  protected void bufferOutput(final int format, final short[] buffer, final int sampleRate) {
    synchronized(this) {
      if(this.sink != null && this.sink.canBuffer()) this.sink.queue(format, buffer, sampleRate);
    }
  }

  protected void bufferOutput(final int format, final float[] buffer, final int sampleRate) {
    synchronized(this) {
      if(this.sink != null && this.sink.canBuffer()) this.sink.queue(format, buffer, sampleRate);
    }
  }

  protected void play() {
    if(this.sink != null) this.sink.play();
  }

  protected void stop() {
    this.active = false;
    if(this.sink != null) this.sink.stop();
  }

  protected void setActive(final boolean active) { this.active = active; }
  public boolean isActive() { return this.active; }
  public float getPosition() { return this.sink == null ? 0.0f : this.sink.position(); }
}
