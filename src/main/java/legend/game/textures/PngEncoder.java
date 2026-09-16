package legend.game.textures;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface PngEncoder {
  byte[] encode(ByteBuffer rgba, int width, int height);
}
