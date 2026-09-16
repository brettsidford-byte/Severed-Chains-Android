package legend.game.textures;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class AndroidPngEncoder implements PngEncoder {
  @Override
  public byte[] encode(final ByteBuffer rgba, final int width, final int height) {
    final ByteBuffer source = rgba.duplicate();
    source.position(0);
    final int[] argb = new int[Math.multiplyExact(width, height)];
    for(int i = 0; i < argb.length; i++) {
      final int r = source.get() & 0xff;
      final int g = source.get() & 0xff;
      final int b = source.get() & 0xff;
      final int a = source.get() & 0xff;
      argb[i] = a << 24 | r << 16 | g << 8 | b;
    }
    final Bitmap bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    if(!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
      bitmap.recycle();
      throw new IllegalStateException("Android failed to encode PNG");
    }
    bitmap.recycle();
    return output.toByteArray();
  }
}
