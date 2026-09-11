package legend.severedchains.android;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * Android lifecycle owner for the future native Severed Chains engine.
 *
 * <p>This is deliberately separate from the Activity and GLSurfaceView. It
 * coordinates the three prerequisites for engine startup: extracted game data,
 * an Android GLES surface, and the GL-thread renderer. The desktop
 * GameEngine.start() cannot be called here yet because its RenderEngine still
 * owns the LWJGL/JavaFX window loop.</p>
 */
public final class AndroidEngineHost {
    private static final String TAG = "SeveredChains";

    public enum State {
        WAITING_FOR_DATA,
        WAITING_FOR_SURFACE,
        READY_FOR_ENGINE,
        RUNNING_RENDER_BRIDGE,
        FAILED
    }

    private volatile State state = State.WAITING_FOR_DATA;
    private volatile boolean surfaceReady;
    private volatile boolean startRequested;
    private volatile String failure;
    private final AndroidGameFrameLoop frameLoop = new AndroidGameFrameLoop();

    public void requestStart() {
        startRequested = true;
        updateState();
    }

    public void onSurfaceCreated(final AssetManager assets) {
        surfaceReady = true;
        Log.i(TAG, "Android engine host received GLES surface");
        updateState();
    }

    public void onSurfaceDestroyed() {
        surfaceReady = false;
        if (state == State.RUNNING_RENDER_BRIDGE) {
            frameLoop.stop();
            state = State.WAITING_FOR_SURFACE;
        }
    }

    public void onFrame() {
        if (state == State.READY_FOR_ENGINE && startRequested) {
            state = State.RUNNING_RENDER_BRIDGE;
            frameLoop.start();
            Log.i(TAG, "Android engine host entered render-bridge phase");
        }
        if (state == State.RUNNING_RENDER_BRIDGE) {
            frameLoop.tick();
        }
    }

    public AndroidGameFrameLoop frameLoop() {
        return frameLoop;
    }

    public State state() {
        return state;
    }

    public String describe() {
        final String error = failure;
        if (error != null) {
            return "Android engine host failed: " + error;
        }
        return "Android engine host: " + state.name().toLowerCase().replace('_', ' ')
            + " (" + frameLoop.describe() + ")";
    }

    private void updateState() {
        if (!startRequested) {
            return;
        }
        if (!AndroidEngineSession.isGameDataReady()) {
            state = State.WAITING_FOR_DATA;
            return;
        }
        if (!surfaceReady) {
            state = State.WAITING_FOR_SURFACE;
            return;
        }
        state = State.READY_FOR_ENGINE;
    }
}
