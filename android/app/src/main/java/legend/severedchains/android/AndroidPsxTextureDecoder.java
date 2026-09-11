package legend.severedchains.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Converts PS1 15-bit BGR pixels into Android GLES RGBA bytes. */
public final class AndroidPsxTextureDecoder {
    private AndroidPsxTextureDecoder() {
    }

    public static ByteBuffer decodeBgr555(final ByteBuffer source, final int width, final int height) {
        if (width <= 0 || height <= 0 || source.remaining() < width * height * 2) {
            throw new IllegalArgumentException("Invalid PS1 texture buffer");
        }

        final ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder());
        final ByteBuffer pixels = source.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < width * height; i++) {
            final int pixel = pixels.getShort() & 0xffff;
            final int r = expand5(pixel & 0x1f);
            final int g = expand5(pixel >>> 5 & 0x1f);
            final int b = expand5(pixel >>> 10 & 0x1f);
            final int a = (pixel & 0x8000) == 0 ? 0 : 255;
            rgba.put((byte)r).put((byte)g).put((byte)b).put((byte)a);
        }
        rgba.position(0);
        return rgba;
    }

    private static int expand5(final int value) {
        return value << 3 | value >>> 2;
    }
}
