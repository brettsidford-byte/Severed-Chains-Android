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
            final int primitiveOffset = objectBase + data.getInt(objectBase + 16);
            final int primitiveCount = data.getInt(objectBase + 20);
            if (vertexCount < 3 || primitiveCount < 1 || vertexOffset < 0
                || vertexOffset + 24 > bytes.length || primitiveOffset < 0
                || primitiveOffset + 4 > bytes.length) {
                return "extracted TMD mesh: unsupported header";
            }

            final int header = data.getInt(primitiveOffset);
            final int packetCount = header & 0xffff;
            final int packetSize = primitivePacketSize(header);
            final int packetOffset = primitiveOffset + 4;
            if (packetCount < 1 || packetOffset + packetSize > bytes.length) {
                return "extracted TMD mesh: unsupported primitive";
            }

            final int primitiveId = header >>> 24;
            final boolean gradated = (header & 0x0004_0000) != 0;
            final boolean normals = (header & 0x0001_0000) == 0;
            final boolean quad = (primitiveId & 0x8) != 0;
            final boolean textured = (primitiveId & 0x4) != 0;
            final boolean lit = (primitiveId & 0x1) == 0;
            final int primitiveVertices = quad ? 4 : 3;
            final int[] vertexIndices = new int[primitiveVertices];
            final int[] u = new int[primitiveVertices];
            final int[] v = new int[primitiveVertices];
            int clut = 0;
            int tpage = 0;
            int read = packetOffset;

            if (textured) {
                for (int i = 0; i < primitiveVertices; i++) {
                    u[i] = data.get(read) & 0xff;
                    v[i] = data.get(read + 1) & 0xff;
                    final int pageOrClut = data.getShort(read + 2) & 0xffff;
                    if (i == 0) clut = pageOrClut;
                    if (i == 1) tpage = pageOrClut;
                    read += 4;
                }
            }
            if (gradated || !lit) {
                read += primitiveVertices * 4;
            } else if (!textured) {
                read += 4;
            }

            for (int i = 0; i < primitiveVertices; i++) {
                if (lit && normals) read += 2;
                if (read + 2 > packetOffset + packetSize) {
                    return "extracted TMD mesh: truncated primitive";
                }
                vertexIndices[i] = data.getShort(read) & 0xffff;
                read += 2;
            }

            final float[] rawX = new float[3];
            final float[] rawY = new float[3];
            final float[] rawZ = new float[3];
            float scale = 1.0f;
            for (int i = 0; i < 3; i++) {
                final int vertexIndex = vertexIndices[i];
                if (vertexIndex >= vertexCount
                    || vertexOffset + vertexIndex * 8 + 6 >= bytes.length) {
                    return "extracted TMD mesh: invalid vertex index";
                }
                final int offset = vertexOffset + vertexIndex * 8;
                rawX[i] = data.getShort(offset);
                rawY[i] = data.getShort(offset + 2);
                rawZ[i] = data.getShort(offset + 4);
                scale = Math.max(scale, Math.max(Math.abs(rawX[i]),
                    Math.max(Math.abs(rawY[i]), Math.abs(rawZ[i]))));
            }

            final float[] vertices = new float[3 * 16];
            for (int i = 0; i < 3; i++) {
                final int offset = i * 16;
                vertices[offset] = rawX[i] / scale * 0.7f;
                vertices[offset + 1] = rawY[i] / scale * 0.7f;
                vertices[offset + 2] = 0.0f;
                vertices[offset + 3] = 1.0f;
                vertices[offset + 4] = 0.0f;
                vertices[offset + 5] = 0.0f;
                vertices[offset + 6] = 1.0f;
                vertices[offset + 7] = u[i] / 256.0f;
                vertices[offset + 8] = v[i] / 256.0f;
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
            Log.i(TAG, "Extracted TMD mesh submitted: " + tmdPath + ", vertices=" + vertexCount
                + ", primitive=0x" + Integer.toHexString(header)
                + ", textured=" + textured + ", tpage=" + tpage + ", clut=" + clut);
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
    private static int primitivePacketSize(final int command) {
        final int primitiveId = command >>> 24;
        final boolean gradated = (command & 0x0004_0000) != 0;
        final boolean normals = (command & 0x0001_0000) == 0;
        final boolean shaded = (primitiveId & 0x10) != 0;
        final boolean quad = (primitiveId & 0x8) != 0;
        final boolean textured = (primitiveId & 0x4) != 0;
        final boolean lit = (primitiveId & 0x1) == 0;
        if (textured && gradated || !textured && !lit) {
            throw new IllegalArgumentException("unsupported primitive type");
        }

        final int vertexCount = quad ? 4 : 3;
        int bytes = vertexCount * 2;
        if (normals) bytes += (shaded ? vertexCount : 1) * 2;
        if (gradated || !lit) bytes += vertexCount * 4;
        else if (!textured) bytes += 4;
        if (textured) bytes += vertexCount * 4;
        return (bytes + 3) & ~3;
    }

}
