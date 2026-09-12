package legend.severedchains.android;

import android.opengl.GLES30;
import android.opengl.GLES32;
import android.util.Log;

import java.nio.ByteBuffer;

/** GLES resource operations kept outside the game-facing renderer lifecycle. */
public final class AndroidGlesResources {
    private static final String TAG = "SeveredChains";

    private AndroidGlesResources() {
    }

    public static int createProgram(final String vertexSource, final String fragmentSource) {
        return createProgram(vertexSource, null, fragmentSource);
    }

    public static int createProgram(final String vertexSource, final String geometrySource,
                                    final String fragmentSource) {
        final int vertex = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        final int geometry = geometrySource == null ? 0
            : compileShader(GLES32.GL_GEOMETRY_SHADER, geometrySource);
        final int fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertex == 0 || (geometrySource != null && geometry == 0) || fragment == 0) {
            if (vertex != 0) GLES30.glDeleteShader(vertex);
            if (geometry != 0) GLES30.glDeleteShader(geometry);
            if (fragment != 0) GLES30.glDeleteShader(fragment);
            return 0;
        }

        final int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertex);
        if (geometry != 0) GLES30.glAttachShader(program, geometry);
        GLES30.glAttachShader(program, fragment);
        GLES30.glLinkProgram(program);
        GLES30.glDeleteShader(vertex);
        if (geometry != 0) GLES30.glDeleteShader(geometry);
        GLES30.glDeleteShader(fragment);

        final int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "Android GLES program link failed: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    public static int uniformBlockIndex(final int program, final String blockName) {
        return GLES30.glGetUniformBlockIndex(program, blockName);
    }

    public static boolean bindUniformBlock(final int program, final String blockName,
                                           final int binding) {
        final int index = uniformBlockIndex(program, blockName);
        if (index == GLES30.GL_INVALID_INDEX) return false;
        GLES30.glUniformBlockBinding(program, index, binding);
        return true;
    }

    public static void deleteProgram(final int program) {
        if (program != 0) GLES30.glDeleteProgram(program);
    }

    public static int createRgbaTexture(final ByteBuffer pixels, final int width, final int height,
                                        final boolean linearFiltering) {
        final int[] handles = new int[1];
        GLES30.glGenTextures(1, handles, 0);
        final int texture = handles[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
        final int filter = linearFiltering ? GLES30.GL_LINEAR : GLES30.GL_NEAREST;
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            GLES30.glDeleteTextures(1, handles, 0);
            return 0;
        }
        return texture;
    }

    public static void deleteTexture(final int texture) {
        if (texture == 0) return;
        GLES30.glDeleteTextures(1, new int[]{texture}, 0);
    }

    private static int compileShader(final int type, final String source) {
        final int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        final int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == GLES30.GL_FALSE) {
            Log.e(TAG, "Android GLES shader failed: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
