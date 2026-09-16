package legend.core.audio;

import java.nio.ByteBuffer;

public interface AudioSink {
  boolean canBuffer();
  void processBuffers();
  void queue(int format, ByteBuffer data, int sampleRate);
  void queue(int format, short[] data, int sampleRate);
  void queue(int format, float[] data, int sampleRate);
  void play();
  void stop();
  float position();
  void destroy();
}
