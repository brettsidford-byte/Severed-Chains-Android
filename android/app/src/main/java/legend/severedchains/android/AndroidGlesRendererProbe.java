package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Validates the GLES 3 features used by Severed Chains before the full
 * renderer backend is connected. This runs on the GL thread and does not
 * change the game's render resolution.
 */
public final class AndroidGlesRendererProbe {
    private static final String TAG = "SeveredChains";

    private AndroidGlesRendererProbe() {
    }

    public static String run() {
        final int[] value = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, value, 0);
        final int maxTextureSize = value[0];
        GLES30.glGetIntegerv(GLES30.GL_MAX_UNIFORM_BLOCK_SIZE, value, 0);
        final int maxUniformBlockSize = value[0];

        final int program = createProgram();
        final boolean shaders = program != 0;
        if (program != 0) {
            GLES30.glDeleteProgram(program);
        }

        final boolean framebuffer = testFramebuffer();
        final boolean integerTexture = testIntegerTexture();

        final String result = "GLES requirements: shaders=" + shaders
            + ", framebuffer=" + framebuffer
            + ", integerTexture=" + integerTexture
            + ", maxTexture=" + maxTextureSize
            + ", maxUniformBlock=" + maxUniformBlockSize;
        Log.i(TAG, result);
        return result;
    }

    private static int createProgram() {
        final String vertex = "#version 300 es\n"
            + "layout(location=0) in vec4 position;\n"
            + "layout(std140) uniform TestBlock { mat4 transform; };\n"
            + "void main() { gl_Position = transform * position; }\n";
        final String fragment = "#version 300 es\n"
            + "precision mediump float;\n"
            + "out vec4 colour;\n"
            + "void main() { colour = vec4(1.0); }\n";

        final int vertexShader = compile(GLES30.GL_VERTEX_SHADER, vertex);
        final int fragmentShader = compile(GLES30.GL_FRAGMENT_SHADER, fragment);
        if (vertexShader == 0 || fragmentShader == 0) {
            if (vertexShader != 0) GLES30.glDeleteShader(vertexShader);
            if (fragmentShader != 0) GLES30.glDeleteShader(fragmentShader);
            return 0;
        }

        final int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, new int[1], 0);
        final int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "GLES test program link failed: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);
            return 0;
        }

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);
        return program;
    }

    private static int compile(final int type, final String source) {
        final int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        final int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "GLES test shader failed: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static boolean testFramebuffer() {
        final int[] framebuffer = new int[1];
        final int[] texture = new int[1];
        GLES30.glGenFramebuffers(1, framebuffer, 0);
        GLES30.glGenTextures(1, texture, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, 64, 64, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, texture[0], 0);
        final boolean complete = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            == GLES30.GL_FRAMEBUFFER_COMPLETE;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glDeleteTextures(1, texture, 0);
        GLES30.glDeleteFramebuffers(1, framebuffer, 0);
        if (!complete) {
            Log.e(TAG, "GLES framebuffer incomplete: 0x"
                + Integer.toHexString(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)));
        }
        return complete;
    }

    private static boolean testIntegerTexture() {
        final int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R32UI, 8, 8, 0,
            GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_INT, null);
        final boolean valid = GLES30.glGetError() == GLES30.GL_NO_ERROR;
        GLES30.glDeleteTextures(1, texture, 0);
        if (!valid) {
            Log.e(TAG, "GLES integer texture rejected by device");
        }
        return valid;
    }
}
