package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import java.io.IOException;
import java.nio.FloatBuffer;

/** Android GLES shader-program resource used by the future RenderApi adapter. */
public final class AndroidGlesShaderResource {
    private final int program;
    private boolean deleted;

    private AndroidGlesShaderResource(final int program) {
        this.program = program;
    }

    public static AndroidGlesShaderResource load(final AssetManager assets,
                                                 final String vertexPath,
                                                 final String fragmentPath) throws IOException {
        final int program = AndroidGlesResources.createProgram(
            AndroidGlesShaderSource.load(assets, vertexPath),
            AndroidGlesShaderSource.load(assets, fragmentPath));
        return program == 0 ? null : new AndroidGlesShaderResource(program);
    }

    public void use() {
        if (deleted) throw new IllegalStateException("Shader has been deleted");
        GLES30.glUseProgram(program);
    }

    public int uniform(final String name) {
        if (deleted) throw new IllegalStateException("Shader has been deleted");
        return GLES30.glGetUniformLocation(program, name);
    }

    public void setFloat(final String name, final float value) {
        GLES30.glUniform1f(uniform(name), value);
    }

    public void setVec2(final String name, final float x, final float y) {
        GLES30.glUniform2f(uniform(name), x, y);
    }

    public void setVec3(final String name, final float x, final float y, final float z) {
        GLES30.glUniform3f(uniform(name), x, y, z);
    }

    public void setVec4(final String name, final float x, final float y,
                        final float z, final float w) {
        GLES30.glUniform4f(uniform(name), x, y, z, w);
    }

    public void setInt(final String name, final int value) {
        GLES30.glUniform1i(uniform(name), value);
    }

    public void setMatrix4(final String name, final float[] values) {
        if (values.length != 16) throw new IllegalArgumentException("Expected 4x4 matrix");
        GLES30.glUniformMatrix4fv(uniform(name), 1, false, values, 0);
    }

    public void setMatrix4(final String name, final FloatBuffer values) {
        if (values.remaining() < 16) throw new IllegalArgumentException("Expected 4x4 matrix");
        GLES30.glUniformMatrix4fv(uniform(name), 1, false, values);
    }

    public int uniformBlock(final String name) {
        if (deleted) throw new IllegalStateException("Shader has been deleted");
        return GLES30.glGetUniformBlockIndex(program, name);
    }

    public void bindUniformBlock(final String name, final int binding) {
        final int index = uniformBlock(name);
        if (index == GLES30.GL_INVALID_INDEX) {
            throw new IllegalArgumentException("Missing uniform block: " + name);
        }
        GLES30.glUniformBlockBinding(program, index, binding);
    }

    public int program() {
        return program;
    }

    public void delete() {
        if (!deleted) {
            GLES30.glDeleteProgram(program);
            deleted = true;
        }
    }
}
