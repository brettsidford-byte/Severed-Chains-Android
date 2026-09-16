package legend.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Platform-neutral allocation for buffers passed to native rendering and audio APIs. */
public final class DirectBuffers {
  private DirectBuffers() { }

  public static ByteBuffer bytes(final int count) {
    return ByteBuffer.allocateDirect(count).order(ByteOrder.nativeOrder());
  }

  public static FloatBuffer floats(final int count) {
    return bytes(Math.multiplyExact(count, Float.BYTES)).asFloatBuffer();
  }

  public static IntBuffer ints(final int count) {
    return bytes(Math.multiplyExact(count, Integer.BYTES)).asIntBuffer();
  }

  public static ShortBuffer shorts(final int count) {
    return bytes(Math.multiplyExact(count, Short.BYTES)).asShortBuffer();
  }
}
