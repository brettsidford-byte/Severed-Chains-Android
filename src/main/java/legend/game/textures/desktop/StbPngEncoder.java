package legend.game.textures.desktop;

import legend.core.memory.types.IntRef;
import legend.game.textures.PngEncoder;
import legend.game.unpacker.ExpandableFileData;
import legend.game.unpacker.FileData;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;

import java.nio.ByteBuffer;

public final class StbPngEncoder implements PngEncoder {
  @Override
  public byte[] encode(final ByteBuffer buffer, final int width, final int height) {
    final FileData compressed = new ExpandableFileData(buffer.capacity());
    final IntRef offset = new IntRef();
    final STBIWriteCallback callback = STBIWriteCallback.create((context, data, size) -> {
      final ByteBuffer newData = STBIWriteCallback.getData(data, size);
      compressed.write(0, newData, offset.get(), newData.limit());
      offset.add(newData.limit());
    });
    STBImageWrite.stbi_write_png_to_func(callback, 0L, width, height, STBImage.STBI_rgb_alpha, buffer, width * 4);
    callback.free();
    final byte[] out = new byte[offset.get()];
    compressed.read(0, out, 0, out.length);
    return out;
  }
}
