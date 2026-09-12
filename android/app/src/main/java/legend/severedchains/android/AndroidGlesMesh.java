package legend.severedchains.android;

import android.opengl.GLES30;
import android.opengl.GLES32;
import legend.core.renderer.BufferUsage;
import legend.core.renderer.Mesh;
import legend.core.renderer.Translucency;
import legend.core.renderer.VertexOrder;

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
    private final boolean useIndices;
    private final int primitiveMode;
    private final int bufferUsage;
    private float[] vertexData;
    private boolean textured;
    private boolean translucent;
    private Translucency translucencyMode;
    private boolean deleted;

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int indexCount) {
        this(vao, vbo, ebo, indexCount, false);
    }

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int indexCount,
                            final boolean intIndices) {
        this(vao, vbo, ebo, indexCount, intIndices, true, GLES30.GL_TRIANGLES,
            GLES30.GL_STATIC_DRAW, null, false, false, null);
    }

    private AndroidGlesMesh(final int vao, final int vbo, final int ebo, final int count,
                            final boolean intIndices, final boolean useIndices,
                            final int primitiveMode, final int bufferUsage,
                            final float[] vertexData, final boolean textured,
                            final boolean translucent, final Translucency translucencyMode) {
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        this.indexCount = count;
        this.intIndices = intIndices;
        this.useIndices = useIndices;
        this.primitiveMode = primitiveMode;
        this.bufferUsage = bufferUsage;
        this.vertexData = vertexData;
        this.textured = textured;
        this.translucent = translucent;
        this.translucencyMode = translucencyMode;
    }

    /** Creates the indexed mesh shape used by the upstream RenderApi. */
    public static AndroidGlesMesh create(final String name, final VertexOrder vertexOrder,
                                         final float[] vertices, final int[] indices,
                                         final boolean textured, final boolean translucent,
                                         final Translucency translucencyMode,
                                         final BufferUsage usage) {
        final int[] handles = new int[1];
        GLES30.glGenVertexArrays(1, handles, 0);
        final int vao = handles[0];
        GLES30.glGenBuffers(1, handles, 0);
        final int vbo = handles[0];
        GLES30.glGenBuffers(1, handles, 0);
        final int ebo = handles[0];
        final int glUsage = toUsage(usage);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES,
            directFloats(vertices), glUsage);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * Integer.BYTES,
            directInts(indices), glUsage);
        GLES30.glBindVertexArray(0);

        return new AndroidGlesMesh(vao, vbo, ebo, indices.length, true, true,
            toMode(vertexOrder), glUsage, vertices, textured, translucent, translucencyMode);
    }

    /** Creates the non-indexed mesh shape used by the upstream RenderApi. */
    public static AndroidGlesMesh create(final String name, final VertexOrder vertexOrder,
                                         final float[] vertices, final int vertexCount,
                                         final boolean textured, final boolean translucent,
                                         final Translucency translucencyMode,
                                         final BufferUsage usage) {
        final int[] handle = new int[1];
        GLES30.glGenVertexArrays(1, handle, 0);
        final int vao = handle[0];
        GLES30.glGenBuffers(1, handle, 0);
        final int vbo = handle[0];
        final int glUsage = toUsage(usage);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES,
            directFloats(vertices), glUsage);
        GLES30.glBindVertexArray(0);

        return new AndroidGlesMesh(vao, vbo, 0, vertexCount, false, false,
            toMode(vertexOrder), glUsage, vertices, textured, translucent, translucencyMode);
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
        final AndroidGlesMesh mesh = new AndroidGlesMesh(vao, vbo, ebo, indices.length, true);
        mesh.vertexData = vertices;
        mesh.textured = true;
        return mesh;
    }

    /** Uploads the complete vertex stream for dynamic/streaming meshes. */
    public void updateVertices(final float[] vertices, final boolean streaming) {
        if (deleted) return;
        vertexData = vertices;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        final FloatBuffer data = directFloats(vertices);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, data,
            streaming ? GLES30.GL_STREAM_DRAW : GLES30.GL_DYNAMIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        if (deleted) return;
        if (useIndices) {
            draw(primitiveMode);
        } else {
            GLES30.glBindVertexArray(vao);
            GLES30.glDrawArrays(primitiveMode, 0, indexCount);
            GLES30.glBindVertexArray(0);
        }
    }

    public void draw(final int primitiveMode) {
        if (!useIndices) {
            GLES30.glBindVertexArray(vao);
            GLES30.glDrawArrays(primitiveMode, 0, indexCount);
            GLES30.glBindVertexArray(0);
            return;
        }
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
        if (deleted || vertexData == null) return;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0,
            vertexData.length * Float.BYTES, directFloats(vertexData));
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void delete() {
        if (deleted) return;
        deleted = true;
        GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
        GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
        if (ebo != 0) {
            GLES30.glDeleteBuffers(1, new int[]{ebo}, 0);
        }
    }

    @Override
    public void attribute(final int index, final long offset, final int size, final int stride) {
        final long byteOffset = offset * Float.BYTES;
        final long byteStride = (long) stride * Float.BYTES;
        if (offset < 0 || byteOffset > Integer.MAX_VALUE || byteStride > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Android GLES attribute offset is outside the supported range");
        }
        GLES30.glBindVertexArray(vao);
        GLES30.glEnableVertexAttribArray(index);
        GLES30.glVertexAttribPointer(index, size, GLES30.GL_FLOAT, false,
            (int) byteStride, (int) byteOffset);
        GLES30.glBindVertexArray(0);
    }

    @Override
    public void draw(final int start, final int count) {
        if (deleted || count <= 0) return;
        GLES30.glBindVertexArray(vao);
        if (useIndices) {
            GLES30.glDrawRangeElements(primitiveMode, start, start + count - 1, count,
                intIndices ? GLES30.GL_UNSIGNED_INT : GLES30.GL_UNSIGNED_SHORT, 0);
        } else {
            GLES30.glDrawArrays(primitiveMode, start, count);
        }
        GLES30.glBindVertexArray(0);
    }

    @Override
    public float[] vertices() {
        return vertexData;
    }

    @Override
    public boolean textured() {
        return textured;
    }

    @Override
    public boolean translucent() {
        return translucent;
    }

    @Override
    public Translucency translucencyMode() {
        return translucencyMode;
    }

    public void destroy() {
        delete();
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

    private static int toMode(final VertexOrder order) {
        return switch (order) {
            case TRIANGLES -> GLES30.GL_TRIANGLES;
            case TRIANGLES_ADJACENCY -> GLES32.GL_TRIANGLES_ADJACENCY;
            case TRIANGLE_STRIP -> GLES30.GL_TRIANGLE_STRIP;
            case LINES -> GLES30.GL_LINES;
            case LINE_LOOP -> GLES30.GL_LINE_LOOP;
            case LINE_STRIP -> GLES30.GL_LINE_STRIP;
        };
    }

    private static int toUsage(final BufferUsage usage) {
        return switch (usage) {
            case STREAMING -> GLES30.GL_STREAM_DRAW;
            case STATIC -> GLES30.GL_STATIC_DRAW;
            case DYNAMIC -> GLES30.GL_DYNAMIC_DRAW;
        };
    }

    private static void attribute(final int location, final int size, final int stride,
                                  final int offset) {
        GLES30.glEnableVertexAttribArray(location);
        GLES30.glVertexAttribPointer(location, size, GLES30.GL_FLOAT, false, stride, offset);
    }
}
