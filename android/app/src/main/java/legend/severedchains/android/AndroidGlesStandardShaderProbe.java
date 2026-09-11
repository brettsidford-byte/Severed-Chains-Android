package legend.severedchains.android;

import android.content.res.AssetManager;
import android.util.Log;

import java.util.Arrays;

/** GL-thread probe for the actual standard shader attribute and UBO contract. */
public final class AndroidGlesStandardShaderProbe {
    private static final String TAG = "SeveredChains";

    private AndroidGlesStandardShaderProbe() {
    }

    public static String run(final AssetManager assets) {
        try {
            final AndroidGlesStandardShader shader = AndroidGlesStandardShader.load(assets);
            final boolean ready = shader.isReady();
            AndroidGlesUniformBuffer[] buffers = new AndroidGlesUniformBuffer[0];
            boolean buffersReady = false;
            if (ready) {
                final int[] sizes = {128, 10240, 16384, 16384, 16, 16};
                buffers = new AndroidGlesUniformBuffer[sizes.length];
                for (int i = 0; i < sizes.length; i++) {
                    buffers[i] = AndroidGlesUniformBuffer.create(sizes[i], i);
                }
                buffersReady = true;
                Log.i(TAG, "Standard shader uniform buffers bound: " + Arrays.toString(sizes));
            }
            if (ready && buffersReady) {
                Log.i(TAG, "Upstream standard shader bindings verified");
            } else {
                Log.e(TAG, "Upstream standard shader bindings failed");
            }
            for (final AndroidGlesUniformBuffer buffer : buffers) {
                if (buffer != null) buffer.destroy();
            }
            shader.delete();
            return "standard shader bindings: " + (ready && buffersReady ? "verified" : "failed");
        } catch (final Exception ex) {
            Log.e(TAG, "Unable to bind upstream standard shader", ex);
            return "standard shader bindings: unavailable";
        }
    }
}
