package legend.core.audio.opus.desktop;

import legend.core.DirectBuffers;
import legend.core.audio.opus.OpusDecoder;
import legend.game.unpacker.FileData;
import org.lwjgl.util.opus.OpusFile;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class LwjglOpusDecoder implements OpusDecoder {
  private ByteBuffer source;
  private long handle;
  private int channels;

  @Override
  public void open(final FileData data) {
    this.source = DirectBuffers.bytes(data.size());
    this.source.put(data.getBytes()).flip();
    final IntBuffer error = DirectBuffers.ints(1);
    this.handle = OpusFile.op_open_memory(this.source, error);
    if(error.get(0) != 0) throw new IllegalArgumentException("Error opening Opus XA file: 0x" + Integer.toHexString(error.get(0)));
    this.channels = OpusFile.op_channel_count(this.handle, -1);
  }

  @Override public int channels() { return this.channels; }
  @Override public long sampleCount() { return OpusFile.op_pcm_total(this.handle, -1) * this.channels; }
  @Override public int read(final short[] pcm) {
    final ShortBuffer output = DirectBuffers.shorts(pcm.length);
    final int frames = OpusFile.op_read(this.handle, output, null);
    output.position(0);
    final int samples = Math.max(0, Math.min(pcm.length, frames * this.channels));
    output.get(pcm, 0, samples);
    return samples;
  }
  @Override public void close() { if(this.handle != 0) { OpusFile.op_free(this.handle); this.handle = 0; this.source = null; } }
}
