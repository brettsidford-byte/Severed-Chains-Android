package legend.severedchains.android;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads build-time-precompiled GLSL ES shaders from the APK asset tree. */
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
            .replaceAll("projectionMode == 1(?![.\\d])", "projectionMode == 1.0")
            .replaceAll("projectionMode == 2(?![.\\d])", "projectionMode == 2.0")
            .replaceAll("discardTranslucency == 1(?![.\\d])", "discardTranslucency == 1.0")
            .replaceAll("discardTranslucency == 2(?![.\\d])", "discardTranslucency == 2.0")
            .replaceAll("alpha != -1(?![.\\d])", "alpha != -1.0")
            .replaceAll("useTextureAlpha != 0(?![.\\d])", "useTextureAlpha != 0.0")
            .replaceAll("useTextureAlpha == 0(?![.\\d])", "useTextureAlpha == 0.0");
        if (typedSource.startsWith(version)) {
            String result = typedSource.replaceAll(
                "(?m)^precision\\s+(?:lowp|mediump|highp)\\s+int;\\s*", "");
            result = result.replaceAll(
                "(?m)^precision\\s+(?:lowp|mediump|highp)\\s+float;\\s*", "");
            if (!result.contains("precision highp float")) {
                result = result.replaceFirst("(#version " + glesVersion + " es\\s*)",
                    "$1precision highp float;\n");
            }
            result = result.replaceFirst("(#version " + glesVersion + " es\\s*)",
                "$1precision highp int;\n");
            return result;
        }
        return typedSource;
    }
}
