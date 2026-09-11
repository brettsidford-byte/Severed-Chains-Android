package legend.severedchains.android;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads the upstream shader files and applies only the desktop-to-GLES version change. */
public final class AndroidGlesShaderSource {
    private AndroidGlesShaderSource() {
    }

    public static String load(final AssetManager assets, final String shaderPath) throws IOException {
        try (InputStream input = assets.open(shaderPath)) {
            final String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return adapt(source);
        }
    }

    public static String adapt(final String source) {
        final String glesSource = source.replace("#version 330 core", "#version 300 es");
        final String typedSource = glesSource
            .replace("projectionMode == 1", "projectionMode == 1.0")
            .replace("projectionMode == 2", "projectionMode == 2.0")
            .replace("discardTranslucency == 1", "discardTranslucency == 1.0")
            .replace("discardTranslucency == 2", "discardTranslucency == 2.0")
            .replace("translucencyMode == 1", "translucencyMode == 1.0")
            .replace("translucencyMode == 2", "translucencyMode == 2.0")
            .replace("translucencyMode == 3", "translucencyMode == 3.0")
            .replace("alpha != -1", "alpha != -1.0")
            .replace("useTextureAlpha != 0", "useTextureAlpha != 0.0")
            .replace("useTextureAlpha == 0", "useTextureAlpha == 0.0");
        if (typedSource.startsWith("#version 300 es")
            && !typedSource.contains("precision mediump float")) {
            return typedSource.replaceFirst("(#version 300 es\\s*)",
                "$1precision mediump float;\\n");
        }
        return typedSource;
    }
}
