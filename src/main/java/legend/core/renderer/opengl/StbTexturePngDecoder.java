package legend.core.renderer.opengl;

import legend.core.renderer.TexturePngDecoder;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memFree;

/** Desktop PNG decoder retaining the existing STB implementation. */
public final class StbTexturePngDecoder implements TexturePngDecoder {
  @Override
  public DecodedTexture decode(final ByteBuffer encodedImage) {
    try(final MemoryStack stack = stackPush()) {
      final IntBuffer width = stack.mallocInt(1);
      final IntBuffer height = stack.mallocInt(1);
      final IntBuffer components = stack.mallocInt(1);
      final ByteBuffer data = stbi_load_from_memory(encodedImage, width, height, components, 4);
      if(data == null) {
        throw new RuntimeException("Failed to load image: " + stbi_failure_reason());
      }
      return new DecodedTexture(data, width.get(0), height.get(0), () -> memFree(data));
    }
  }
}
