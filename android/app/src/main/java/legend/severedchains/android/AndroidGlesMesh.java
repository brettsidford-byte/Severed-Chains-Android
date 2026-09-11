package legend.severedchains.android;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Interleaved GLES vertex/index mesh owned by the Android render thread. */
public final class AndroidGlesMesh {
    private static final int FLOATS_PER_VERTEX = 5;

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int indexCount) {
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        this.indexCount = indexCount;
    }

    public static AndroidGlesMesh createColouredMesh(final float[] vertices, final short[] indices,
                                                     final int positionLocation,
                                                     final int colourLocation) {
        final FloatBuffer vertexBuffer = directFloats(vertices);
        final ShortBuffer indexBuffer = directShorts(indices);
        final int[] handles = new int[1];

        GLES30.glGenVertexArrays(1, handles, 0);
        final int vao = handles[0];
        GLES30.glGenBuffers(1, handles, 0);
        final int vbo = handles[0];
        GLES30.glGenBuffers(1, handles, 0);
        final int ebo = handles[0];

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES,
            vertexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * Short.BYTES,
            indexBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glEnableVertexAttribArray(positionLocation);
        GLES30.glVertexAttribPointer(positionLocation, 2, GLES30.GL_FLOAT, false,
            FLOATS_PER_VERTEX * Float.BYTES, 0);
        GLES30.glEnableVertexAttribArray(colourLocation);
        GLES30.glVertexAttribPointer(colourLocation, 3, GLES30.GL_FLOAT, false,
            FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GLES30.glBindVertexArray(0);

        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
            GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
            GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
            return null;
        }
        return new AndroidGlesMesh(vao, vbo, ebo, indices.length);
    }

    public void draw() {
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
    }

    public void destroy() {
        GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
        GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
        GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
    }

    private static FloatBuffer directFloats(final float[] values) {
        final FloatBuffer buffer = ByteBuffer.allocateDirect(values.length * Float.BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(values).position(0);
        return buffer;
    }

    private static ShortBuffer directShorts(final short[] values) {
        final ShortBuffer buffer = ByteBuffer.allocateDirect(values.length * Short.BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(values).position(0);
        return buffer;
    }
}
