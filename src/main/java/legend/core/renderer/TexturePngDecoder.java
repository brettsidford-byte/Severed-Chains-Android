package legend.core.renderer;

import java.nio.ByteBuffer;

/** Platform service for decoding encoded PNG bytes into tightly-packed RGBA pixels. */
@FunctionalInterface
public interface TexturePngDecoder {
  DecodedTexture decode(ByteBuffer encodedImage);

  record DecodedTexture(ByteBuffer data, int width, int height, Runnable cleanup)
      implements AutoCloseable {
    public DecodedTexture {
      if(data == null) throw new IllegalArgumentException("Decoded texture data must not be null");
      if(width <= 0 || height <= 0) throw new IllegalArgumentException("Decoded texture dimensions must be positive");
      if(cleanup == null) cleanup = () -> { };
    }

    @Override
    public void close() {
      cleanup.run();
    }
  }
}
