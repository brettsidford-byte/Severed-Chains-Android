package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Function;
import java.util.function.Supplier;

import legend.core.renderer.Shader;
import legend.core.renderer.ShaderOptions;
import legend.core.renderer.ShaderUniformFloat;
import legend.core.renderer.ShaderUniformInt;
import legend.core.renderer.ShaderUniformMat4;
import legend.core.renderer.ShaderUniformVec2;
import legend.core.renderer.ShaderUniformVec3;
import legend.core.renderer.ShaderUniformVec4;

/** Shader-program resource used by Android's upstream RenderApi implementation. */
public final class AndroidGlesShaderResource<Options extends ShaderOptions>
    implements Shader<Options> {
    private final AssetManager assets;
    private final String vertexPath;
    private final String geometryPath;
    private final String fragmentPath;
    private final Function<Shader<Options>, Supplier<Options>> optionsFactory;
    private Supplier<Options> options;
    private int program;
    private boolean deleted;

    private AndroidGlesShaderResource(final AssetManager assets, final String vertexPath,
                                      final String geometryPath, final String fragmentPath,
                                      final Function<Shader<Options>, Supplier<Options>> optionsFactory,
                                      final int program) {
        this.assets = assets;
        this.vertexPath = vertexPath;
        this.geometryPath = geometryPath;
        this.fragmentPath = fragmentPath;
        this.optionsFactory = optionsFactory;
        this.program = program;
        this.options = optionsFactory.apply(this);
    }

    public static AndroidGlesShaderResource<ShaderOptions> load(final AssetManager assets,
                                                                final String vertexPath,
                                                                final String fragmentPath)
        throws IOException {
        return load(assets, vertexPath, fragmentPath, shader -> () -> () -> { });
    }

    public static <Options extends ShaderOptions> AndroidGlesShaderResource<Options> load(
        final AssetManager assets, final String vertexPath, final String fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        final int program = createProgram(assets, vertexPath, null, fragmentPath);
        if (program == 0) {
            throw new IOException("Failed to create GLES shader program " + vertexPath
                + " / " + fragmentPath);
        }
        return new AndroidGlesShaderResource<>(assets, vertexPath, null,
            fragmentPath, optionsFactory, program);
    }

    public static <Options extends ShaderOptions> AndroidGlesShaderResource<Options> load(
        final AssetManager assets, final String vertexPath, final String geometryPath,
        final String fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        final int program = createProgram(assets, vertexPath, geometryPath, fragmentPath);
        if (program == 0) {
            throw new IOException("Failed to create GLES shader program " + vertexPath
                + " / " + geometryPath + " / " + fragmentPath);
        }
        return new AndroidGlesShaderResource<>(assets, vertexPath,
            geometryPath, fragmentPath, optionsFactory, program);
    }

    @Override
    public void reload() throws IOException {
        requireLive();
        final int replacement = createProgram(assets, vertexPath, geometryPath, fragmentPath);
        if (replacement == 0) {
            throw new IOException("Failed to reload GLES shader " + vertexPath + " / "
                + fragmentPath);
        }
        GLES30.glDeleteProgram(program);
        program = replacement;
        options = optionsFactory.apply(this);
    }

    @Override
    public Options makeOptions() {
        requireLive();
        return options.get();
    }

    @Override
    public void bindUniformBlock(final CharSequence name, final int binding) {
        bindUniformBlock(name.toString(), binding);
    }

    @Override
    public void use() {
        requireLive();
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

    @Override
    public void delete() {
        if (!deleted) {
            GLES30.glDeleteProgram(program);
            deleted = true;
        }
    }

    @Override
    public ShaderUniformVec2 uniformVec2(final String name) {
        final int location = uniform(name);
        return new ShaderUniformVec2() {
            @Override
            public void set(final FloatBuffer buffer) {
                GLES30.glUniform2fv(location, 1, buffer);
            }

            @Override
            public void set(final org.joml.Vector2fc vec) {
                set(vec.x(), vec.y());
            }

            @Override
            public void set(final float x, final float y) {
                GLES30.glUniform2f(location, x, y);
            }
        };
    }

    @Override
    public ShaderUniformVec3 uniformVec3(final String name) {
        final int location = uniform(name);
        return new ShaderUniformVec3() {
            @Override
            public void set(final FloatBuffer buffer) {
                GLES30.glUniform3fv(location, 1, buffer);
            }

            @Override
            public void set(final org.joml.Vector3fc vec) {
                set(vec.x(), vec.y(), vec.z());
            }

            @Override
            public void set(final float x, final float y, final float z) {
                GLES30.glUniform3f(location, x, y, z);
            }
        };
    }

    @Override
    public ShaderUniformVec4 uniformVec4(final String name) {
        final int location = uniform(name);
        return new ShaderUniformVec4() {
            @Override
            public void set(final FloatBuffer buffer) {
                GLES30.glUniform4fv(location, 1, buffer);
            }

            @Override
            public void set(final org.joml.Vector4fc vec) {
                set(vec.x(), vec.y(), vec.z(), vec.w());
            }

            @Override
            public void set(final float x, final float y, final float z, final float w) {
                GLES30.glUniform4f(location, x, y, z, w);
            }
        };
    }

    @Override
    public ShaderUniformMat4 uniformMat4(final String name) {
        final int location = uniform(name);
        return new ShaderUniformMat4() {
            @Override
            public void set(final org.joml.Matrix4fc matrix) {
                set(matrix, false);
            }

            @Override
            public void set(final org.joml.Matrix4fc matrix, final boolean transpose) {
                final FloatBuffer values = ByteBuffer.allocateDirect(16 * Float.BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
                matrix.get(values);
                values.position(0);
                set(values, transpose);
            }

            @Override
            public void set(final FloatBuffer matrix) {
                set(matrix, false);
            }

            @Override
            public void set(final FloatBuffer matrix, final boolean transpose) {
                GLES30.glUniformMatrix4fv(location, 1, transpose, matrix);
            }
        };
    }

    @Override
    public ShaderUniformInt uniformInt(final String name) {
        final int location = uniform(name);
        return value -> GLES30.glUniform1i(location, value);
    }

    @Override
    public ShaderUniformFloat uniformFloat(final String name) {
        final int location = uniform(name);
        return value -> GLES30.glUniform1f(location, value);
    }

    private void requireLive() {
        if (deleted) {
            throw new IllegalStateException("Shader has been deleted");
        }
    }

    private static int createProgram(final AssetManager assets, final String vertexPath,
                                     final String geometryPath, final String fragmentPath)
        throws IOException {
        if (geometryPath == null) {
            return AndroidGlesResources.createProgram(
                AndroidGlesShaderSource.load(assets, vertexPath),
                AndroidGlesShaderSource.load(assets, fragmentPath));
        }
        return AndroidGlesResources.createProgram(
            AndroidGlesShaderSource.load(assets, vertexPath, 320),
            AndroidGlesShaderSource.load(assets, geometryPath, 320),
            AndroidGlesShaderSource.load(assets, fragmentPath, 320));
    }
}
