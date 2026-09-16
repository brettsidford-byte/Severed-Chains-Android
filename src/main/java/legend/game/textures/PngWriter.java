package legend.game.textures;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class PngWriter {
  private static PngEncoder encoder;
  private PngWriter() { }

  public static void setEncoder(final PngEncoder encoder) {
    PngWriter.encoder = Objects.requireNonNull(encoder);
  }

  public static byte[] compress(final ByteBuffer buffer, final int width, final int height) {
    if(encoder == null) throw new IllegalStateException("PNG encoder is not installed");
    return encoder.encode(buffer, width, height);
  }

  public static void write(final Path path, final ByteBuffer data, final int width, final int height) throws IOException {
    Files.write(path, compress(data, width, height), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}
