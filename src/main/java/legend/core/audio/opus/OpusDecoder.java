package legend.core.audio.opus;

import legend.game.unpacker.FileData;

public interface OpusDecoder {
  void open(FileData data);
  int channels();
  long sampleCount();
  int read(short[] pcm);
  void close();
}
