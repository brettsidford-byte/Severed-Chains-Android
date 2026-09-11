package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Minimal GLES 3 backend proof for the primitives used by the game renderer. */
public final class AndroidGlesRenderBackend implements AndroidRenderApi {
    private static final String TAG = "SeveredChains";
    private static final int FLOATS_PER_VERTEX = 5;
    private int program;
    private int vao;
    private int vbo;
    private int ebo;
    private int positionLocation;
    private int colourLocation;
    private boolean ready;

    @Override
    public void create() {
        final String vertexSource = "#version 300 es\\n"
            + "layout(location=0) in vec2 position;\\n"
            + "layout(location=1) in vec3 colour;\\n"
            + "out vec3 vertexColour;\\n"
            + "void main() { gl_Position = vec4(position, 0.0, 1.0); vertexColour = colour; }\\n";
        final String fragmentSource = "#version 300 es\\n"
            + "precision mediump float;\\n"
            + "in vec3 vertexColour;\\n"
            + "out vec4 fragmentColour;\\n"
            + "void main() { fragmentColour = vec4(vertexColour, 1.0); }\\n";

        final int vertexShader = compile(GLES30.GL_VERTEX_SHADER, vertexSource);
        final int fragmentShader = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertexShader == 0 || fragmentShader == 0) return;

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        final int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);
        if (linked[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "Android GLES backend link failed: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            program = 0;
            return;
        }

        positionLocation = GLES30.glGetAttribLocation(program, "position");
        colourLocation = GLES30.glGetAttribLocation(program, "colour");

        final float[] vertices = {
            -0.75f, -0.55f, 0.15f, 0.50f, 0.95f,
             0.75f, -0.55f, 0.20f, 0.85f, 0.35f,
             0.75f,  0.55f, 0.95f, 0.65f, 0.15f,
            -0.75f,  0.55f, 0.75f, 0.25f, 0.90f
        };
        final short[] indices = {0, 1, 2, 0, 2, 3};
        final FloatBuffer vertexBuffer = directFloats(vertices);
        final ShortBuffer indexBuffer = directShorts(indices);

        final int[] handles = new int[1];
        GLES30.glGenVertexArrays(1, handles, 0);
        vao = handles[0];
        GLES30.glGenBuffers(1, handles, 0);
        vbo = handles[0];
        GLES30.glGenBuffers(1, handles, 0);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * Float.BYTES,
            vertexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, handles[0]);
        ebo = handles[0];
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * Short.BYTES,
            indexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(positionLocation);
        GLES30.glVertexAttribPointer(positionLocation, 2, GLES30.GL_FLOAT, false,
            FLOATS_PER_VERTEX * Float.BYTES, 0);
        GLES30.glEnableVertexAttribArray(colourLocation);
        GLES30.glVertexAttribPointer(colourLocation, 3, GLES30.GL_FLOAT, false,
            FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        GLES30.glBindVertexArray(0);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        ready = GLES30.glGetError() == GLES30.GL_NO_ERROR;
        Log.i(TAG, "Android GLES backend ready=" + ready);
    }

    @Override
    public void resize(final int width, final int height) {
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void beginFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void draw() {
        if (!ready) return;
        GLES30.glUseProgram(program);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    private static int compile(final int type, final String source) {
        final int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        final int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "Android GLES backend shader failed: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
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
