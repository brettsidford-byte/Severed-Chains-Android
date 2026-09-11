package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;


/** Minimal GLES 3 backend proof for the primitives used by the game renderer. */
public final class AndroidGlesRenderBackend implements AndroidRenderApi {
    private static final String TAG = "SeveredChains";
    private static final int FLOATS_PER_VERTEX = 5;
    private int program;
    private AndroidGlesMesh mesh;
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

        program = AndroidGlesResources.createProgram(vertexSource, fragmentSource);
        if (program == 0) return;

        positionLocation = GLES30.glGetAttribLocation(program, "position");
        colourLocation = GLES30.glGetAttribLocation(program, "colour");

        final float[] vertices = {
            -0.75f, -0.55f, 0.15f, 0.50f, 0.95f,
             0.75f, -0.55f, 0.20f, 0.85f, 0.35f,
             0.75f,  0.55f, 0.95f, 0.65f, 0.15f,
            -0.75f,  0.55f, 0.75f, 0.25f, 0.90f
        };
        final short[] indices = {0, 1, 2, 0, 2, 3};
        mesh = AndroidGlesMesh.createColouredMesh(vertices, indices, positionLocation, colourLocation);
        if (mesh == null) return;

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        final AndroidGlesFrameBuffer framebufferProbe = AndroidGlesFrameBuffer.create(1, 1, true);
        final boolean framebufferReady = framebufferProbe.isComplete();
        framebufferProbe.destroy();
        ready = GLES30.glGetError() == GLES30.GL_NO_ERROR && framebufferReady;
        Log.i(TAG, "Android GLES backend ready=" + ready + ", framebuffer=" + framebufferReady);
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
        mesh.draw();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

}
