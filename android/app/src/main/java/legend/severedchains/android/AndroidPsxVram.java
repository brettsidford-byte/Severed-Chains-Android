package legend.severedchains.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Android-side PS1 VRAM storage.
 *
 * <p>The desktop GPU keeps both 15-bit VRAM and an expanded colour view. Android
 * keeps the source 15-bit values here and expands only the texture region that
 * is uploaded to GLES. This preserves PS1 page/CLUT addressing and avoids a
 * second 1024x512 colour array.</p>
 */
public final class AndroidPsxVram {
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;

    private final short[] pixels = new short[WIDTH * HEIGHT];

    public void clear() {
        java.util.Arrays.fill(this.pixels, (short) 0);
    }

    public void uploadData15(final int x, final int y, final int width, final int height,
                             final ByteBuffer source) {
        checkRect(x, y, width, height);
        if (source.remaining() < width * height * 2) {
            throw new IllegalArgumentException("PS1 VRAM upload is shorter than its rectangle");
        }

        final ByteBuffer input = source.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                this.pixels[(y + row) * WIDTH + x + column] = input.getShort();
            }
        }
    }

    public short getPixel15(final int x, final int y) {
        checkCoordinate(x, y);
        return this.pixels[y * WIDTH + x];
    }

    /**
     * Returns an RGBA texture region in the same 15-bit colour order used by
     * the upstream GPU. The returned buffer is positioned at zero.
     */
    public ByteBuffer readRgba(final int x, final int y, final int width, final int height) {
        checkRect(x, y, width, height);
        final ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder());
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                putRgba(rgba, this.pixels[(y + row) * WIDTH + x + column] & 0xffff);
            }
        }
        rgba.position(0);
        return rgba;
    }

    /**
     * Decodes one PS1 texture sample using the page and CLUT coordinates
     * carried by a textured primitive.
     */
    public int getTexel(final int pageX, final int pageY, final int u, final int v,
                        final int widthDivisor, final int widthMask, final int indexShift, final int indexMask,
                        final int clutX, final int clutY) {
        final int packedX = pageX + u / widthDivisor;
        final int packed = this.pixels[(pageY + v) * WIDTH + packedX] & 0xffff;
        if (indexMask == 0) {
            return packed;
        }
        final int paletteIndex = packed >>> ((u & widthMask) * indexShift) & indexMask;
        return this.pixels[clutY * WIDTH + clutX + paletteIndex] & 0xffff;
    }

    public static void putRgba(final ByteBuffer output, final int pixel) {
        final int r = expand5(pixel & 0x1f);
        final int g = expand5(pixel >>> 5 & 0x1f);
        final int b = expand5(pixel >>> 10 & 0x1f);
        final int a = (pixel & 0x8000) == 0 ? 0 : 255;
        output.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
    }

    private static int expand5(final int value) {
        return value << 3 | value >>> 2;
    }

    private static void checkRect(final int x, final int y, final int width, final int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0
            || x + width > WIDTH || y + height > HEIGHT) {
            throw new IllegalArgumentException("PS1 VRAM rectangle is outside 1024x512 VRAM");
        }
    }

    private static void checkCoordinate(final int x, final int y) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
            throw new IllegalArgumentException("PS1 VRAM coordinate is outside 1024x512 VRAM");
        }
    }
}
