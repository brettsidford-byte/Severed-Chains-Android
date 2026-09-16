package legend.severedchains.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import legend.core.renderer.TexturePngDecoder;

/** Android Bitmap-backed decoder for upstream TextureBuilder PNG requests. */
public final class AndroidTexturePngDecoder implements TexturePngDecoder {
    @Override
    public DecodedTexture decode(final ByteBuffer encodedImage) {
        final ByteBuffer source = encodedImage.duplicate();
        final byte[] encoded = new byte[source.remaining()];
        source.get(encoded);
        final Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length);
        if (bitmap == null) throw new IllegalArgumentException("Android could not decode PNG data");

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] argb = new int[Math.multiplyExact(width, height)];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        bitmap.recycle();

        final ByteBuffer rgba = ByteBuffer.allocateDirect(Math.multiplyExact(argb.length, 4))
            .order(ByteOrder.nativeOrder());
        for (final int pixel : argb) {
            rgba.put((byte) ((pixel >>> 16) & 0xff));
            rgba.put((byte) ((pixel >>> 8) & 0xff));
            rgba.put((byte) (pixel & 0xff));
            rgba.put((byte) ((pixel >>> 24) & 0xff));
        }
        rgba.flip();
        return new DecodedTexture(rgba, width, height, null);
    }
}
