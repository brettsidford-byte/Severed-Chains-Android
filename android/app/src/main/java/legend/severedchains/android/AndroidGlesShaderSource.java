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
        return source.replace("#version 330 core", "#version 300 es");
    }
}
