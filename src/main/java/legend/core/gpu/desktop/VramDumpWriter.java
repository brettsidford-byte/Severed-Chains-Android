package legend.core.gpu.desktop;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class VramDumpWriter {
  private VramDumpWriter() { }

  public static void write(final int[] data, final int width, final int height) {
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, width, height, data, 0, width);
    try {
      ImageIO.write(image, "png", new File("dump.png"));
    } catch(final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
