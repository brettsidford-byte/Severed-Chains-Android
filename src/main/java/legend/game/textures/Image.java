package legend.game.textures;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import legend.core.renderer.TextureBuilder;
import legend.core.renderer.TexturePngDecoder;

public class Image {
  public final byte[] data;
  public final int width;
  public final int height;

  public Image(final byte[] data, final int width, final int height) {
    this.data = data;
    this.width = width;
    this.height = height;
  }

  public static Image load(final Path path) {
    try(final TexturePngDecoder.DecodedTexture decoded = TextureBuilder.decodePng(path)) {
      final ByteBuffer data = decoded.data().duplicate();
      final byte[] decompressed = new byte[data.remaining()];
      data.get(decompressed);
      return new Image(decompressed, decoded.width(), decoded.height());
    }
  }
}
