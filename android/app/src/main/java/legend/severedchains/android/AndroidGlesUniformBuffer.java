package legend.severedchains.android;

import android.opengl.GLES30;

import java.nio.ByteBuffer;

/** std140 uniform buffer owned and updated on the GL thread. */
public final class AndroidGlesUniformBuffer {
    private final int handle;
    private final int binding;
    private final int size;

    private AndroidGlesUniformBuffer(final int handle, final int binding, final int size) {
        this.handle = handle;
        this.binding = binding;
        this.size = size;
    }

    public static AndroidGlesUniformBuffer create(final int size, final int binding) {
        if (size <= 0) throw new IllegalArgumentException("Uniform buffer size must be positive");
        final int[] handles = new int[1];
        GLES30.glGenBuffers(1, handles, 0);
        final AndroidGlesUniformBuffer buffer =
            new AndroidGlesUniformBuffer(handles[0], binding, size);
        buffer.upload(ByteBuffer.allocateDirect(size));
        return buffer;
    }

    public void upload(final ByteBuffer data) {
        if (data.remaining() > size) {
            throw new IllegalArgumentException("Uniform data exceeds buffer size");
        }
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, handle);
        GLES30.glBufferData(GLES30.GL_UNIFORM_BUFFER, size, data, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, binding, handle);
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0);
    }

    public int binding() {
        return binding;
    }

    public int size() {
        return size;
    }

    public void destroy() {
        GLES30.glDeleteBuffers(1, new int[]{handle}, 0);
    }
}
