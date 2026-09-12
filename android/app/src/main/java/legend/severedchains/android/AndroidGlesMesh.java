package legend.severedchains.android;

import android.opengl.GLES30;
import legend.core.renderer.Mesh;
import legend.core.renderer.Translucency;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Interleaved GLES vertex/index mesh owned by the Android render thread. */
public final class AndroidGlesMesh implements Mesh {
    private static final int FLOATS_PER_VERTEX = 5;

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;
    private final boolean intIndices;
    private float[] vertexData;
    private boolean textured;

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int indexCount) {
        this(vao, vbo, ebo, indexCount, false);
    }

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int indexCount,
                            final boolean intIndices) {
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        this.indexCount = indexCount;
        this.intIndices = intIndices;
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

    public static AndroidGlesMesh createTexturedMesh(final float[] vertices, final short[] indices,
                                                      final int positionLocation,
                                                      final int colourLocation,
                                                      final int textureLocation) {
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
            7 * Float.BYTES, 0);
        GLES30.glEnableVertexAttribArray(colourLocation);
        GLES30.glVertexAttribPointer(colourLocation, 3, GLES30.GL_FLOAT, false,
            7 * Float.BYTES, 2 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(textureLocation);
        GLES30.glVertexAttribPointer(textureLocation, 2, GLES30.GL_FLOAT, false,
            7 * Float.BYTES, 5 * Float.BYTES);
        GLES30.glBindVertexArray(0);

        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
            GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
            GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
            return null;
        }
        return new AndroidGlesMesh(vao, vbo, ebo, indices.length);
    }

    public static AndroidGlesMesh createStandardMesh(final float[] vertices, final short[] indices,
                                                       final int positionLocation,
                                                       final int normalLocation,
                                                       final int uvLocation,
                                                       final int tpageLocation,
                                                       final int clutLocation,
                                                       final int colourLocation,
                                                       final int flagsLocation) {
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

        final int stride = 16 * Float.BYTES;
        GLES30.glEnableVertexAttribArray(positionLocation);
        GLES30.glVertexAttribPointer(positionLocation, 4, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(normalLocation);
        GLES30.glVertexAttribPointer(normalLocation, 3, GLES30.GL_FLOAT, false, stride, 4 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(uvLocation);
        GLES30.glVertexAttribPointer(uvLocation, 2, GLES30.GL_FLOAT, false, stride, 7 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(tpageLocation);
        GLES30.glVertexAttribPointer(tpageLocation, 1, GLES30.GL_FLOAT, false, stride, 9 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(clutLocation);
        GLES30.glVertexAttribPointer(clutLocation, 1, GLES30.GL_FLOAT, false, stride, 10 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(colourLocation);
        GLES30.glVertexAttribPointer(colourLocation, 4, GLES30.GL_FLOAT, false, stride, 11 * Float.BYTES);
        GLES30.glEnableVertexAttribArray(flagsLocation);
        GLES30.glVertexAttribPointer(flagsLocation, 1, GLES30.GL_FLOAT, false, stride, 15 * Float.BYTES);
        GLES30.glBindVertexArray(0);

        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
            GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
            GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
            return null;
        }
        return new AndroidGlesMesh(vao, vbo, ebo, indices.length, true);
    }

    /** Standard upstream vertex layout using the renderer's 32-bit index contract. */
    public static AndroidGlesMesh createStandardMesh(final float[] vertices, final int[] indices,
                                                       final int positionLocation,
                                                       final int normalLocation,
                                                       final int uvLocation,
                                                       final int tpageLocation,
                                                       final int clutLocation,
                                                       final int colourLocation,
                                                       final int flagsLocation) {
        final FloatBuffer vertexBuffer = directFloats(vertices);
        final IntBuffer indexBuffer = directInts(indices);
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
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * Integer.BYTES,
            indexBuffer, GLES30.GL_STATIC_DRAW);

        final int stride = 16 * Float.BYTES;
        attribute(positionLocation, 4, stride, 0);
        attribute(normalLocation, 3, stride, 4 * Float.BYTES);
        attribute(uvLocation, 2, stride, 7 * Float.BYTES);
        attribute(tpageLocation, 1, stride, 9 * Float.BYTES);
        attribute(clutLocation, 1, stride, 10 * Float.BYTES);
        attribute(colourLocation, 4, stride, 11 * Float.BYTES);
        attribute(flagsLocation, 1, stride, 15 * Float.BYTES);
        GLES30.glBindVertexArray(0);

        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
            GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
            GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
            return null;
        }
        final AndroidGlesMesh mesh = new AndroidGlesMesh(vao, vbo, ebo, indices.length);
        mesh.vertexData = vertices.clone();
        mesh.textured = true;
        return mesh;
    }

    /** Uploads the complete vertex stream for dynamic/streaming meshes. */
    public void updateVertices(final float[] vertices, final boolean streaming) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        final FloatBuffer data = directFloats(vertices);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, data,
            streaming ? GLES30.GL_STREAM_DRAW : GLES30.GL_DYNAMIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        draw(GLES30.GL_TRIANGLES);
    }

    public void draw(final int primitiveMode) {
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(primitiveMode, indexCount,
            intIndices ? GLES30.GL_UNSIGNED_INT : GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
    }

    /** Draws a contiguous indexed range using the upstream primitive mode. */
    public void drawRange(final int primitiveMode, final int start, final int count) {
        if (start < 0 || count < 0 || start + count > indexCount) {
            throw new IllegalArgumentException("Mesh draw range is outside the index buffer");
        }
        GLES30.glBindVertexArray(vao);
        final int offset = start * (intIndices ? Integer.BYTES : Short.BYTES);
        GLES30.glDrawElements(primitiveMode, count,
            intIndices ? GLES30.GL_UNSIGNED_INT : GLES30.GL_UNSIGNED_SHORT, offset);
        GLES30.glBindVertexArray(0);
    }

    @Override
    public void update() {
    }

    @Override
    public void delete() {
        destroy();
    }

    @Override
    public void attribute(final int index, final long offset, final int size, final int stride) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Android GLES attribute offset is outside the supported range");
        }
        GLES30.glBindVertexArray(vao);
        GLES30.glEnableVertexAttribArray(index);
        GLES30.glVertexAttribPointer(index, size, GLES30.GL_FLOAT, false, stride, (int) offset);
        GLES30.glBindVertexArray(0);
    }

    @Override
    public void draw(final int start, final int count) {
        drawRange(GLES30.GL_TRIANGLES, start, count);
    }

    @Override
    public float[] vertices() {
        return vertexData == null ? null : vertexData.clone();
    }

    @Override
    public boolean textured() {
        return textured;
    }

    @Override
    public boolean translucent() {
        return false;
    }

    @Override
    public Translucency translucencyMode() {
        return null;
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

    private static IntBuffer directInts(final int[] values) {
        final IntBuffer buffer = ByteBuffer.allocateDirect(values.length * Integer.BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.put(values).position(0);
        return buffer;
    }

    private static void attribute(final int location, final int size, final int stride,
                                  final int offset) {
        GLES30.glEnableVertexAttribArray(location);
        GLES30.glVertexAttribPointer(location, size, GLES30.GL_FLOAT, false, stride, offset);
    }
}
