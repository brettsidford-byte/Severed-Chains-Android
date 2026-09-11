package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.util.Log;

import java.io.IOException;

/** Draws a real standard-shader mesh using the upstream vertex contract. */
public final class AndroidGlesStandardMeshProbe {
    private static final String TAG = "SeveredChains";

    private AndroidGlesStandardMeshProbe() {
    }

    public static String run(final AssetManager assets) {
        AndroidGlesStandardShader shader = null;
        AndroidGlesStandardUniforms uniforms = null;
        AndroidGlesMesh mesh = null;
        try {
            shader = AndroidGlesStandardShader.load(assets);
            if (!shader.isReady()) return "standard mesh: shader unavailable";
            uniforms = AndroidGlesStandardUniforms.createDefaults();

            final float[] vertices = {
                -0.65f, -0.50f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f,  0.0f, 0.0f,  1.0f, 0.2f, 0.8f, 1.0f,  4.0f,
                 0.65f, -0.50f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  1.0f, 0.0f,  0.0f, 0.0f,  0.2f, 1.0f, 0.4f, 1.0f,  4.0f,
                 0.65f,  0.50f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  1.0f, 1.0f,  0.0f, 0.0f,  1.0f, 0.4f, 0.2f, 1.0f,  4.0f,
                -0.65f,  0.50f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 1.0f,  0.0f, 0.0f,  0.8f, 0.2f, 0.8f, 1.0f,  4.0f
            };
            final short[] indices = {0, 1, 2, 0, 2, 3};
            mesh = AndroidGlesMesh.createStandardMesh(vertices, indices,
                shader.attribute(0), shader.attribute(1), shader.attribute(2),
                shader.attribute(3), shader.attribute(4), shader.attribute(5),
                shader.attribute(6));
            if (mesh == null) return "standard mesh: buffer creation failed";

            GLES30.glUseProgram(shader.program());
            set2(shader, "clutOverride", 0.0f, 0.0f);
            set2(shader, "tpageOverride", 0.0f, 0.0f);
            set1(shader, "modelIndex", 0.0f);
            set3(shader, "recolour", 1.0f, 1.0f, 1.0f);
            set2(shader, "uvOffset", 0.0f, 0.0f);
            set1(shader, "translucency", 0.0f);
            set1(shader, "discardTranslucency", 0.0f);
            set1(shader, "alpha", -1.0f);
            set1(shader, "useTextureAlpha", 0.0f);
            mesh.draw();

            final boolean ok = GLES30.glGetError() == GLES30.GL_NO_ERROR;
            Log.i(TAG, "Upstream standard mesh draw=" + ok);
            return "standard mesh: " + (ok ? "submitted" : "failed");
        } catch (final IOException ex) {
            Log.e(TAG, "Unable to load standard mesh shader", ex);
            return "standard mesh: unavailable";
        } finally {
            if (mesh != null) mesh.destroy();
            if (uniforms != null) uniforms.destroy();
            if (shader != null) shader.delete();
        }
    }

    private static void set1(final AndroidGlesStandardShader shader, final String name, final float value) {
        final int location = shader.uniform(name);
        if (location >= 0) GLES30.glUniform1f(location, value);
    }

    private static void set2(final AndroidGlesStandardShader shader, final String name,
                             final float x, final float y) {
        final int location = shader.uniform(name);
        if (location >= 0) GLES30.glUniform2f(location, x, y);
    }

    private static void set3(final AndroidGlesStandardShader shader, final String name,
                             final float x, final float y, final float z) {
        final int location = shader.uniform(name);
        if (location >= 0) GLES30.glUniform3f(location, x, y, z);
    }
}
