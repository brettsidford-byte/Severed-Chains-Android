package legend.severedchains.android;

import android.content.res.AssetManager;
import android.util.Log;

/** Compiles the upstream simple shader pair after the Android GLSL adaptation. */
public final class AndroidGlesShaderProbe {
    private static final String TAG = "SeveredChains";

    private AndroidGlesShaderProbe() {
    }

    public static String run(final AssetManager assets) {
        try {
            final String vertex = AndroidGlesShaderSource.load(assets, "gfx/shaders/simple.vsh");
            final String fragment = AndroidGlesShaderSource.load(assets, "gfx/shaders/simple.fsh");
            final int program = AndroidGlesResources.createProgram(vertex, fragment);
            if (program == 0) {
                return "upstream simple shader: failed";
            }
            AndroidGlesResources.deleteProgram(program);
            Log.i(TAG, "Upstream simple shader compiled after GLES adaptation");
            return "upstream simple shader: compiled";
        } catch (final Exception ex) {
            Log.e(TAG, "Unable to load upstream shader assets", ex);
            return "upstream simple shader: unavailable";
        }
    }
}
