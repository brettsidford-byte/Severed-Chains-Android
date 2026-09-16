package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;


import legend.core.renderer.Texture;
import legend.core.renderer.TextureDataFormat;
import legend.core.renderer.TextureDataType;
import legend.core.renderer.TextureInternalFormat;

/** Android GLES implementation of the upstream Texture lifecycle and format contract. */
public final class AndroidGlesTextureResource extends Texture {
    private static final String TAG = "SeveredChains";
    private static final int[] currentTextures = new int[32];
    private static int currentActiveTexture;

    private final int id;
    private final TextureInternalFormat internalFormat;
    private final TextureDataFormat dataFormat;
    private final TextureDataType dataType;
    private final boolean minFilter;
    private final boolean magFilter;
    private final boolean wrapS;
    private final boolean wrapT;
    private boolean actuallyDeleted;

    private AndroidGlesTextureResource(final int id, final String name, final int width,
                                       final int height,
                                       final TextureInternalFormat internalFormat,
                                       final TextureDataFormat dataFormat,
                                       final TextureDataType dataType,
                                       final boolean minFilter, final boolean magFilter,
                                       final boolean wrapS, final boolean wrapT) {
        super(name, width, height);
        this.id = id;
        this.internalFormat = internalFormat;
        this.dataFormat = dataFormat;
        this.dataType = dataType;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.wrapS = wrapS;
        this.wrapT = wrapT;
    }

    public static AndroidGlesTextureResource create(final Buffer data,
                                                    final String name,
                                                    final int width, final int height,
                                                    final TextureInternalFormat internalFormat,
                                                    final TextureDataFormat dataFormat,
                                                    final TextureDataType dataType,
                                                    final boolean minFilter,
                                                    final boolean magFilter,
                                                    final boolean wrapS,
                                                    final boolean wrapT) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        final int[] handles = new int[1];
        GLES30.glGenTextures(1, handles, 0);
        final AndroidGlesTextureResource texture = new AndroidGlesTextureResource(
            handles[0], name, width, height, internalFormat, dataFormat, dataType,
            minFilter, magFilter, wrapS, wrapT);
        texture.use();
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
            minFilter ? GLES30.GL_LINEAR : GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER,
            magFilter ? GLES30.GL_LINEAR : GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
            wrapS ? GLES30.GL_REPEAT : GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
            wrapT ? GLES30.GL_REPEAT : GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, toInternalFormat(internalFormat, dataType),
            width, height, 0, toDataFormat(dataFormat), toDataType(dataType), data);
        final int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            texture.performDelete();
            throw new IllegalStateException("Failed to create Android GLES texture " + name
                + ": 0x" + Integer.toHexString(error));
        }
        return texture;
    }

    public static void unbindAll() {
        for (int unit = 0; unit < currentTextures.length; unit++) {
            if (currentTextures[unit] != 0) {
                currentTextures[unit] = 0;
                activate(unit);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            }
        }
    }

    @Override
    public void data(final int x, final int y, final int width, final int height,
                     final TextureDataType uploadType, final ByteBuffer data) {
        validateUpdate(x, y, width, height);
        use();
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, width, height,
            toDataFormat(dataFormat), toDataType(uploadType), data);
    }

    @Override
    public void data(final int x, final int y, final int width, final int height,
                     final TextureDataType uploadType, final int[] data) {
        validateUpdate(x, y, width, height);
        if (data == null || (long) width * height > data.length) {
            throw new IllegalArgumentException("Texture data is smaller than the update region");
        }
        final IntBuffer buffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.put(data).position(0);
        use();
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, width, height,
            toDataFormat(dataFormat), toDataType(uploadType), buffer);
    }

    @Override
    public void use(final int activeTexture) {
        if (activeTexture < 0 || activeTexture >= currentTextures.length) {
            throw new IllegalArgumentException("Invalid texture unit " + activeTexture);
        }
        if (actuallyDeleted) Log.w(TAG, name + " used after being deleted");
        activate(activeTexture);
        if (currentTextures[activeTexture] != id) {
            currentTextures[activeTexture] = id;
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id);
        }
    }

    @Override
    public void use() {
        use(0);
    }

    @Override public TextureInternalFormat internalFormat() { return internalFormat; }
    @Override public TextureDataFormat dataFormat() { return dataFormat; }
    @Override public TextureDataType dataType() { return dataType; }
    @Override public boolean minFilter() { return minFilter; }
    @Override public boolean magFilter() { return magFilter; }
    @Override public boolean wrapS() { return wrapS; }
    @Override public boolean wrapT() { return wrapT; }

    @Override
    protected void performDelete() {
        if (actuallyDeleted) return;
        for (int unit = 0; unit < currentTextures.length; unit++) {
            if (currentTextures[unit] == id) currentTextures[unit] = 0;
        }
        GLES30.glDeleteTextures(1, new int[]{id}, 0);
        actuallyDeleted = true;
    }

    public int id() { return id; }

    private static void activate(final int unit) {
        if (currentActiveTexture != unit) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit);
            currentActiveTexture = unit;
        }
    }

    private void validateUpdate(final int x, final int y,
                                final int width, final int height) {
        if (actuallyDeleted) throw new IllegalStateException("Texture has been deleted");
        if (x < 0 || y < 0 || width < 0 || height < 0
            || x + width > this.width || y + height > this.height) {
            throw new IllegalArgumentException("Texture update is outside the texture bounds");
        }
    }

    private static int toInternalFormat(final TextureInternalFormat format,
                                        final TextureDataType dataType) {
        return switch (format) {
            case RGB_8 -> GLES30.GL_RGB8;
            case RGBA_8 -> GLES30.GL_RGBA8;
            case R_32_UINT -> GLES30.GL_R32UI;
            case DEPTH_COMPONENT -> dataType == TextureDataType.FLOAT
                ? GLES30.GL_DEPTH_COMPONENT32F : GLES30.GL_DEPTH_COMPONENT24;
        };
    }

    private static int toDataFormat(final TextureDataFormat format) {
        return switch (format) {
            case RGB -> GLES30.GL_RGB;
            case RGBA -> GLES30.GL_RGBA;
            case RED_INT -> GLES30.GL_RED_INTEGER;
            case DEPTH_COMPONENT -> GLES30.GL_DEPTH_COMPONENT;
        };
    }

    private static int toDataType(final TextureDataType type) {
        return switch (type) {
            case UBYTE -> GLES30.GL_UNSIGNED_BYTE;
            case UINT -> GLES30.GL_UNSIGNED_INT;
            case FLOAT -> GLES30.GL_FLOAT;
        };
    }

}
