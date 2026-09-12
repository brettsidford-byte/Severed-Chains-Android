package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

/** GL-thread lifecycle owned by the Android surface. The real Severed Chains renderer
 * will be attached here once its RenderApi backend is connected. */
public final class AndroidEngineRenderBridge {
    private static final String TAG = "SeveredChains";
    private static final int LOGICAL_WIDTH = 640;
    private static final int LOGICAL_HEIGHT = 480;
    private volatile boolean created;
    private volatile int width;
    private volatile int height;
    private final AndroidRenderApi backend = new AndroidGlesRenderBackend();

    public void onSurfaceCreated() {
        created = true;
        Log.i(TAG, "Android engine render bridge created: " + GLES30.glGetString(GLES30.GL_VERSION));
        backend.create();
    }

    public void onSurfaceDestroyed() {
        if (!created) return;
        backend.destroy();
        created = false;
        width = 0;
        height = 0;
        Log.i(TAG, "Android engine render bridge destroyed with EGL surface");
    }

    public void onSurfaceChanged(final int width, final int height) {
        this.width = width;
        this.height = height;
        final float surfaceAspect = (float) width / (float) height;
        final float logicalAspect = (float) LOGICAL_WIDTH / (float) LOGICAL_HEIGHT;
        final int viewportWidth;
        final int viewportHeight;
        final int viewportX;
        final int viewportY;
        if (surfaceAspect > logicalAspect) {
            viewportHeight = height;
            viewportWidth = Math.round(height * logicalAspect);
            viewportX = (width - viewportWidth) / 2;
            viewportY = 0;
        } else {
            viewportWidth = width;
            viewportHeight = Math.round(width / logicalAspect);
            viewportX = 0;
            viewportY = (height - viewportHeight) / 2;
        }
        backend.resize(viewportX, viewportY, viewportWidth, viewportHeight);
        Log.i(TAG, "Android engine render bridge resized: " + width + "x" + height);
        Log.i(TAG, "Android engine logical viewport: " + viewportX + "," + viewportY
            + " " + viewportWidth + "x" + viewportHeight + " (4:3)");
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
