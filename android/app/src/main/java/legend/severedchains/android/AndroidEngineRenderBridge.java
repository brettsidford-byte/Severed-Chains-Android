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

    public void onSurfaceCreated() {
        created = true;
        Log.i(TAG, "Android engine render bridge created: " + GLES30.glGetString(GLES30.GL_VERSION));
    }

    public void onSurfaceChanged(final int width, final int height) {
        this.width = width;
        this.height = height;
        GLES30.glViewport(0, 0, width, height);
        Log.i(TAG, "Android engine render bridge resized: " + width + "x" + height);
    }

    public void onDrawFrame() {
        if (!created) return;
        // The existing game renderer will replace this clear when the Android RenderApi is attached.
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
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
