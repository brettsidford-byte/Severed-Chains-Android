package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import legend.core.GamePaths;

/** Minimal real-data bridge: reads the first extracted TMD vertex table. */
public final class AndroidExtractedTmdMeshProbe {
    private static final String TAG = "SeveredChains";

    private AndroidExtractedTmdMeshProbe() {
    }

    public static String run(final AssetManager assets) {
        try {
            final Path tmdPath;
            try (Stream<Path> paths = Files.walk(GamePaths.files())) {
                tmdPath = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".tmd"))
                    .sorted(Comparator.comparing(Path::toString))
                    .findFirst().orElse(null);
            }
            if (tmdPath == null) return "extracted TMD mesh: none found";

            final byte[] bytes = Files.readAllBytes(tmdPath);
            final ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            final int objectBase = 8;
            final int vertexOffset = objectBase + data.getInt(objectBase);
            final int vertexCount = data.getInt(objectBase + 4);
            if (vertexCount < 3 || vertexOffset < 0 || vertexOffset + 24 > bytes.length) {
                return "extracted TMD mesh: unsupported header";
            }

            final short[] x = new short[3];
            final short[] y = new short[3];
            final short[] z = new short[3];
            float scale = 1.0f;
            for (int i = 0; i < 3; i++) {
                final int offset = vertexOffset + i * 8;
                x[i] = data.getShort(offset);
                y[i] = data.getShort(offset + 2);
                z[i] = data.getShort(offset + 4);
                scale = Math.max(scale, Math.max(Math.abs(x[i]), Math.max(Math.abs(y[i]), Math.abs(z[i]))));
            }

            final float[] vertices = new float[3 * 16];
            for (int i = 0; i < 3; i++) {
                final int offset = i * 16;
                vertices[offset] = x[i] / scale * 0.7f;
                vertices[offset + 1] = y[i] / scale * 0.7f;
                vertices[offset + 2] = 0.0f;
                vertices[offset + 3] = 1.0f;
                vertices[offset + 4] = 0.0f;
                vertices[offset + 5] = 0.0f;
                vertices[offset + 6] = 1.0f;
                vertices[offset + 7] = 0.0f;
                vertices[offset + 8] = 0.0f;
                vertices[offset + 9] = 0.0f;
                vertices[offset + 10] = 0.0f;
                vertices[offset + 11] = 1.0f;
                vertices[offset + 12] = 0.8f;
                vertices[offset + 13] = 0.8f;
                vertices[offset + 14] = 0.8f;
                vertices[offset + 15] = 4.0f;
            }

            final AndroidGlesStandardShader shader = AndroidGlesStandardShader.load(assets);
            if (!shader.isReady()) {
                shader.delete();
                return "extracted TMD mesh: shader unavailable";
            }
            final AndroidGlesStandardUniforms uniforms = AndroidGlesStandardUniforms.createDefaults();
            final AndroidGlesMesh mesh = AndroidGlesMesh.createStandardMesh(vertices, new short[]{0, 1, 2},
                shader.attribute(0), shader.attribute(1), shader.attribute(2),
                shader.attribute(3), shader.attribute(4), shader.attribute(5), shader.attribute(6));
            if (mesh == null) {
                uniforms.destroy();
                shader.delete();
                return "extracted TMD mesh: buffer creation failed";
            }

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

            final boolean submitted = GLES30.glGetError() == GLES30.GL_NO_ERROR;
            Log.i(TAG, "Extracted TMD mesh submitted: " + tmdPath + ", vertices=" + vertexCount);
            mesh.destroy();
            uniforms.destroy();
            shader.delete();
            return "extracted TMD mesh: " + (submitted ? "submitted" : "failed");
        } catch (final IOException | RuntimeException ex) {
            Log.e(TAG, "Unable to submit extracted TMD mesh", ex);
            return "extracted TMD mesh: unavailable";
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
