package legend.severedchains.android;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads upstream shader files and applies the narrowly-scoped desktop-to-GLES changes. */
public final class AndroidGlesShaderSource {
    private AndroidGlesShaderSource() {
    }

    public static String load(final AssetManager assets, final String shaderPath) throws IOException {
        return load(assets, shaderPath, 300);
    }

    public static String load(final AssetManager assets, final String shaderPath,
                              final int glesVersion) throws IOException {
        try (InputStream input = assets.open(shaderPath)) {
            final String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return adapt(source, glesVersion);
        }
    }

    public static String adapt(final String source) {
        return adapt(source, 300);
    }

    public static String adapt(final String source, final int glesVersion) {
        if (glesVersion != 300 && glesVersion != 320) {
            throw new IllegalArgumentException("Only GLSL ES 3.00 and 3.20 are supported");
        }
        final String normalised = source.replace("\uFEFF", "").stripLeading();
        final String version = "#version " + glesVersion + " es";
        final String glesSource = normalised.replaceFirst(
            "(?m)^#version\\s+330\\s+core\\s*$", version);
        final String typedSource = glesSource
            .replace("projectionMode == 1", "projectionMode == 1.0")
            .replace("projectionMode == 2", "projectionMode == 2.0")
            .replace("discardTranslucency == 1", "discardTranslucency == 1.0")
            .replace("discardTranslucency == 2", "discardTranslucency == 2.0")
            .replace("alpha != -1", "alpha != -1.0")
            .replace("useTextureAlpha != 0", "useTextureAlpha != 0.0")
            .replace("useTextureAlpha == 0", "useTextureAlpha == 0.0");
        if (typedSource.startsWith(version)) {
            String result = typedSource;
            if (!result.contains("precision mediump float")) {
                result = result.replaceFirst("(#version " + glesVersion + " es\\s*)",
                    "$1precision mediump float;\n");
            }
            if (!result.contains("precision mediump int")) {
                result = result.replaceFirst("(#version " + glesVersion + " es\\s*)",
                    "$1precision mediump int;\n");
            }
            return result;
        }
        return typedSource;
    }
}
