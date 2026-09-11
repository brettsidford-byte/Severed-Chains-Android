package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

/** GL-thread lifecycle owned by the Android surface. The real Severed Chains renderer
 * will be attached here once its RenderApi backend is connected. */
public final class AndroidEngineRenderBridge {
    private static final String TAG = "SeveredChains";
    private volatile boolean created;
    private volatile int width;
    private volatile int height;
    private final AndroidRenderApi backend = new AndroidGlesRenderBackend();

    public void onSurfaceCreated() {
        created = true;
        Log.i(TAG, "Android engine render bridge created: " + GLES30.glGetString(GLES30.GL_VERSION));
        backend.create();
    }

    public void onSurfaceChanged(final int width, final int height) {
        this.width = width;
        this.height = height;
        backend.resize(width, height);
        Log.i(TAG, "Android engine render bridge resized: " + width + "x" + height);
    }

    public void onDrawFrame() {
        if (!created) return;
        backend.beginFrame();
        backend.draw();
    }

    public boolean isBackendReady() {
        return backend.isReady();
    }

    public boolean isCreated() {
        return created;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
