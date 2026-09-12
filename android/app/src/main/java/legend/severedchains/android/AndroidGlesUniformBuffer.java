package legend.severedchains.android;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import legend.core.renderer.ShaderUniformBuffer;

/** std140 uniform buffer owned and updated on the GL thread. */
public final class AndroidGlesUniformBuffer implements ShaderUniformBuffer {
    private final int handle;
    private final int binding;
    private final int size;
    private boolean deleted;

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
        requireLive();
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

    @Override
    public void set(final FloatBuffer buffer) {
        set(0L, buffer);
    }

    @Override
    public void set(final long offset, final FloatBuffer buffer) {
        requireLive();
        final long byteCount = (long) buffer.remaining() * Float.BYTES;
        if (offset < 0 || offset > Integer.MAX_VALUE
            || byteCount > Integer.MAX_VALUE || offset + byteCount > size) {
            throw new IllegalArgumentException("Uniform update is outside the buffer bounds");
        }
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, handle);
        GLES30.glBufferSubData(GLES30.GL_UNIFORM_BUFFER, (int) offset, (int) byteCount, buffer);
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void delete() {
        if (!deleted) {
            GLES30.glDeleteBuffers(1, new int[]{handle}, 0);
            deleted = true;
        }
    }

    public void destroy() {
        delete();
    }

    private void requireLive() {
        if (deleted) {
            throw new IllegalStateException("Uniform buffer has been deleted");
        }
    }
}
