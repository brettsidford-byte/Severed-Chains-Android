package legend.severedchains.android;

import android.content.res.AssetManager;
import android.util.Log;


/** GL-thread probe for the actual standard shader attribute and UBO contract. */
public final class AndroidGlesStandardShaderProbe {
    private static final String TAG = "SeveredChains";

    private AndroidGlesStandardShaderProbe() {
    }

    public static String run(final AssetManager assets) {
        try {
            final AndroidGlesStandardShader shader = AndroidGlesStandardShader.load(assets);
            final boolean ready = shader.isReady();
            AndroidGlesStandardUniforms uniforms = null;
            final boolean uniformsReady;
            if (ready) {
                uniforms = AndroidGlesStandardUniforms.createDefaults();
                uniformsReady = true;
                Log.i(TAG, "Standard shader std140 defaults uploaded");
            } else {
                uniformsReady = false;
            }
            if (ready && uniformsReady) {
                Log.i(TAG, "Upstream standard shader bindings verified");
            } else {
                Log.e(TAG, "Upstream standard shader bindings failed");
            }
            if (uniforms != null) uniforms.destroy();
            shader.delete();
            return "standard shader bindings: " + (ready && uniformsReady ? "verified" : "failed");
        } catch (final Exception ex) {
            Log.e(TAG, "Unable to bind upstream standard shader", ex);
            return "standard shader bindings: unavailable";
        }
    }
}
