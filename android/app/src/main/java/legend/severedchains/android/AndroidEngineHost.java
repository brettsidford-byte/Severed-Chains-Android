package legend.severedchains.android;

import android.content.res.AssetManager;
import android.util.Log;

import legend.game.unpacker.Unpacker;

/**
 * Android lifecycle owner for the engine startup boundary.
 *
 * <p>Android copies the user-provided discs into GamePaths.isos(), then the
 * existing upstream unpacker is run internally as part of startup. There is no
 * separate extraction action in the user interface.</p>
 */
public final class AndroidEngineHost {
    private static final String TAG = "SeveredChains";

    public enum State {
        WAITING_FOR_DATA,
        WAITING_FOR_SURFACE,
        PREPARING_GAME_DATA,
        READY_FOR_ENGINE,
        RUNNING_RENDER_BRIDGE,
        FAILED
    }

    private volatile State state = State.WAITING_FOR_DATA;
    private volatile boolean surfaceReady;
    private volatile boolean startRequested;
    private volatile boolean preparationStarted;
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
        if (!startRequested || !surfaceReady) {
            return;
        }

        if (!preparationStarted && !AndroidEngineSession.isGameDataReady()) {
            if (!AndroidEngineSession.hasIsoInput()) {
                state = State.WAITING_FOR_DATA;
                return;
            }
            preparationStarted = true;
            state = State.PREPARING_GAME_DATA;
            startPreparation();
        }

        if (state == State.READY_FOR_ENGINE) {
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
        if (!AndroidEngineSession.isGameDataReady()
            && !AndroidEngineSession.hasIsoInput()) {
            state = State.WAITING_FOR_DATA;
            return;
        }
        if (!surfaceReady) {
            state = State.WAITING_FOR_SURFACE;
            return;
        }
        if (!preparationStarted && !AndroidEngineSession.isGameDataReady()) {
            state = State.PREPARING_GAME_DATA;
            return;
        }
        if (AndroidEngineSession.isGameDataReady()) {
            state = State.READY_FOR_ENGINE;
        }
    }

    private void startPreparation() {
        new Thread(() -> {
            try {
                Unpacker.setStatusListener(status ->
                    Log.i(TAG, "Severed Chains startup: " + status));
                Log.i(TAG, "Starting existing Severed Chains unpacker from Android ISO directory");
                Unpacker.unpack();
                if (AndroidEngineSession.isGameDataReady()) {
                    state = State.READY_FOR_ENGINE;
                    Log.i(TAG, "Severed Chains startup data is ready");
                } else {
                    failure = "startup preparation ended without a completion marker";
                    state = State.FAILED;
                    Log.e(TAG, failure);
                }
            } catch (final Throwable throwable) {
                failure = throwable.toString();
                state = State.FAILED;
                Log.e(TAG, "Severed Chains startup preparation failed", throwable);
            }
        }, "severed-chains-startup").start();
    }
}
