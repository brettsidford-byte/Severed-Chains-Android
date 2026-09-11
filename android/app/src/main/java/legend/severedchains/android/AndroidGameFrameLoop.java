package legend.severedchains.android;

import android.util.Log;

/**
 * Android-owned frame clock for the future Severed Chains game loop.
 *
 * <p>GLSurfaceView supplies the render-thread callback. This class turns that
 * callback into a bounded, monotonic frame interval so game logic can later
 * be advanced without inheriting the desktop window loop.</p>
 */
public final class AndroidGameFrameLoop {
    private static final String TAG = "SeveredChains";
    private static final float MAX_DELTA_SECONDS = 0.25f;

    private long frameCount;
    private long lastNanos;
    private float deltaSeconds;
    private boolean running;

    public void start() {
        lastNanos = System.nanoTime();
        frameCount = 0;
        deltaSeconds = 0.0f;
        running = true;
        Log.i(TAG, "Android game frame loop started");
    }

    public void stop() {
        running = false;
        deltaSeconds = 0.0f;
    }

    public void tick() {
        if (!running) {
            return;
        }
        final long now = System.nanoTime();
        final long elapsed = Math.max(0L, now - lastNanos);
        lastNanos = now;
        deltaSeconds = Math.min(MAX_DELTA_SECONDS, elapsed / 1_000_000_000.0f);
        frameCount++;
    }

    public boolean isRunning() {
        return running;
    }

    public long frameCount() {
        return frameCount;
    }

    public float deltaSeconds() {
        return deltaSeconds;
    }

    public String describe() {
        return "Android game loop: " + (running ? "running" : "stopped")
            + ", frames=" + frameCount
            + ", delta=" + String.format(java.util.Locale.ROOT, "%.4fs", deltaSeconds);
    }
}
