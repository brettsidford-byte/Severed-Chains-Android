package legend.severedchains.android;

import android.opengl.GLES30;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Android GLES texture resource used by the renderer adapter.
 *
 * <p>This keeps Android's array-based GLES handle API behind a small resource
 * object and covers the formats used by the upstream GLES renderer. All
 * methods are intended for the GLSurfaceView render thread.</p>
 */
public final class AndroidGlesTextureResource {
    public enum Format {
        RGB8(GLES30.GL_RGB8, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE),
        RGBA8(GLES30.GL_RGBA8, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE),
        R32_UINT(GLES30.GL_R32UI, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_INT),
        DEPTH(GLES30.GL_DEPTH_COMPONENT24, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT);

        private final int internal;
        private final int external;
        private final int type;

        Format(final int internal, final int external, final int type) {
            this.internal = internal;
            this.external = external;
            this.type = type;
        }
    }

    private final int id;
    private final int width;
    private final int height;
    private final Format format;
    private boolean deleted;

    private AndroidGlesTextureResource(final int id, final int width, final int height,
                                       final Format format) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.format = format;
    }

    public static AndroidGlesTextureResource create(final int width, final int height,
                                                    final Format format, final Buffer data,
                                                    final boolean linear, final boolean repeat) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        final int[] handles = new int[1];
        GLES30.glGenTextures(1, handles, 0);
        final int texture = handles[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
        final int filter = linear ? GLES30.GL_LINEAR : GLES30.GL_NEAREST;
        final int wrap = repeat ? GLES30.GL_REPEAT : GLES30.GL_CLAMP_TO_EDGE;
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, format.internal, width, height, 0,
            format.external, format.type, data);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteTextures(1, handles, 0);
            return null;
        }
        return new AndroidGlesTextureResource(texture, width, height, format);
    }

    public void bind(final int unit) {
        if (deleted) throw new IllegalStateException("Texture has been deleted");
        if (unit < 0 || unit > 31) throw new IllegalArgumentException("Invalid texture unit");
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id);
    }

    public void update(final int x, final int y, final int width, final int height,
                       final Buffer data) {
        if (deleted) throw new IllegalStateException("Texture has been deleted");
        if (x < 0 || y < 0 || width < 0 || height < 0
            || x + width > this.width || y + height > this.height) {
            throw new IllegalArgumentException("Texture update is outside the texture bounds");
        }
        bind(0);
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, width, height,
            format.external, format.type, data);
    }

    /** Uploads packed integer texture data using the upstream texture API shape. */
    public void update(final int x, final int y, final int width, final int height,
                       final int[] data) {
        if (data == null) throw new IllegalArgumentException("Texture data must not be null");
        if ((long) width * height > data.length) {
            throw new IllegalArgumentException("Texture data is smaller than the update region");
        }
        final IntBuffer buffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.put(data).position(0);
        update(x, y, width, height, buffer);
    }

    public int id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Format format() {
        return format;
    }

    public void delete() {
        if (!deleted) {
            GLES30.glDeleteTextures(1, new int[]{id}, 0);
            deleted = true;
        }
    }
}
