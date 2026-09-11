package legend.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class IoHelper {
  private IoHelper() { }
  public static String readString(final ByteBuffer buffer, final int length) {
    final byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.US_ASCII);
  }
}
