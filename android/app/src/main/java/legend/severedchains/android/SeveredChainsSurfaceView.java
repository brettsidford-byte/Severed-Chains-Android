package legend.severedchains.android;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import legend.core.platform.input.InputAxis;
import legend.core.platform.input.InputButton;
import legend.core.platform.AndroidPlatformManager;
import legend.core.GameEngine;

public final class SeveredChainsSurfaceView extends GLSurfaceView {
    private static final String TAG = "SeveredChains";

    private final GameRenderer renderer;
    private final AndroidInputState input = new AndroidInputState();
    private volatile AndroidPlatformManager platformManager;
    private final AtomicBoolean engineStarted = new AtomicBoolean();
    private volatile Thread engineThread;
    private volatile boolean engineRequested;
    private volatile boolean surfaceReady;
    private final Runnable requestScheduledFrame = this::requestRender;

    public SeveredChainsSurfaceView(final Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setEGLContextFactory(new Gles32ContextFactory());
        setEGLConfigChooser(8, 8, 8, 8, 24, 8);
        setPreserveEGLContextOnPause(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        renderer = new GameRenderer();
        setRenderer(renderer);
        // Start continuously so GLSurfaceView creates its EGL surface on every
        // supported Android host. onSurfaceCreated switches to scheduled draws.
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        requestFocus();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (input.onKeyDown(keyCode, event)) {
            final InputButton button = AndroidInputState.buttonForKey(keyCode);
            final AndroidPlatformManager platform = platformManager;
            if (button != null && platform != null) {
                queueEvent(() -> platform.dispatchButtonPressed(button, event.getRepeatCount() > 0));
            }
            return true;
        }
        Log.d(TAG, "Unmapped key down: " + KeyEvent.keyCodeToString(keyCode));
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (input.onKeyUp(keyCode, event)) {
            final InputButton button = AndroidInputState.buttonForKey(keyCode);
            final AndroidPlatformManager platform = platformManager;
            if (button != null && platform != null) {
                queueEvent(() -> platform.dispatchButtonReleased(button));
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        final boolean dpadUpBefore = input.isHeld(InputButton.DPAD_UP);
        final boolean dpadDownBefore = input.isHeld(InputButton.DPAD_DOWN);
        final boolean dpadLeftBefore = input.isHeld(InputButton.DPAD_LEFT);
        final boolean dpadRightBefore = input.isHeld(InputButton.DPAD_RIGHT);
        if (input.onGenericMotion(event)) {
            final float leftX = input.axis(InputAxis.LEFT_X);
            final float leftY = input.axis(InputAxis.LEFT_Y);
            final float rightX = input.axis(InputAxis.RIGHT_X);
            final float rightY = input.axis(InputAxis.RIGHT_Y);
            final float leftTrigger = input.axis(InputAxis.LEFT_TRIGGER);
            final float rightTrigger = input.axis(InputAxis.RIGHT_TRIGGER);
            final boolean dpadUp = input.isHeld(InputButton.DPAD_UP);
            final boolean dpadDown = input.isHeld(InputButton.DPAD_DOWN);
            final boolean dpadLeft = input.isHeld(InputButton.DPAD_LEFT);
            final boolean dpadRight = input.isHeld(InputButton.DPAD_RIGHT);
            final AndroidPlatformManager platform = platformManager;
            if(platform != null) {
                queueEvent(() -> {
                    dispatchButtonTransition(platform, InputButton.DPAD_UP, dpadUpBefore, dpadUp);
                    dispatchButtonTransition(platform, InputButton.DPAD_DOWN, dpadDownBefore, dpadDown);
                    dispatchButtonTransition(platform, InputButton.DPAD_LEFT, dpadLeftBefore, dpadLeft);
                    dispatchButtonTransition(platform, InputButton.DPAD_RIGHT, dpadRightBefore, dpadRight);
                    platform.dispatchAxis(InputAxis.LEFT_X, leftX);
                    platform.dispatchAxis(InputAxis.LEFT_Y, leftY);
                    platform.dispatchAxis(InputAxis.RIGHT_X, rightX);
                    platform.dispatchAxis(InputAxis.RIGHT_Y, rightY);
                    platform.dispatchAxis(InputAxis.LEFT_TRIGGER, leftTrigger);
                    platform.dispatchAxis(InputAxis.RIGHT_TRIGGER, rightTrigger);
                });
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private static void dispatchButtonTransition(final AndroidPlatformManager platform,
                                                 final InputButton button,
                                                 final boolean before,
                                                 final boolean after) {
        if(!before && after) {
            platform.dispatchButtonPressed(button, false);
        } else if(before && !after) {
            platform.dispatchButtonReleased(button);
        }
    }

    public void startEngine() {
        engineRequested = true;
        maybeStartGameEngine();
    }

    /** Stops the upstream loop while the EGL thread is still available for GPU cleanup. */
    public void shutdownEngine() {
        final AndroidPlatformManager platform = platformManager;
        if (platform != null) platform.stop();

        final Thread thread = engineThread;
        if (thread == null || thread == Thread.currentThread()) return;
        try {
            thread.join(5_000L);
            if (thread.isAlive()) Log.w(TAG, "Engine shutdown exceeded five seconds");
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** The upstream renderer's TMD geometry shader requires OpenGL ES 3.2. */
    private static final class Gles32ContextFactory implements EGLContextFactory {
        private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        private static final int EGL_CONTEXT_MINOR_VERSION_KHR = 0x30fb;

        @Override
        public EGLContext createContext(final EGL10 egl, final EGLDisplay display,
                                        final EGLConfig config) {
            final int[] attributes = {
                EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL_CONTEXT_MINOR_VERSION_KHR, 2,
                EGL10.EGL_NONE
            };
            EGLContext context = egl.eglCreateContext(display, config,
                EGL10.EGL_NO_CONTEXT, attributes);
            if(context == null || context == EGL10.EGL_NO_CONTEXT) {
                final int firstError = egl.eglGetError();
                Log.w(TAG, "Explicit GLES 3.2 EGL context request failed (0x"
                    + Integer.toHexString(firstError) + "); requesting the driver's highest GLES 3 context");
                context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                    new int[]{EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE});
            }
            if(context == null || context == EGL10.EGL_NO_CONTEXT) {
                throw new IllegalStateException("OpenGL ES 3 context creation failed: 0x"
                    + Integer.toHexString(egl.eglGetError()));
            }
            return context;
        }

        @Override
        public void destroyContext(final EGL10 egl, final EGLDisplay display,
                                   final EGLContext context) {
            if(!egl.eglDestroyContext(display, context)) {
                Log.w(TAG, "Failed to destroy GLES 3.2 context: 0x"
                    + Integer.toHexString(egl.eglGetError()));
            }
        }
    }

    @Override
    public void onPause() {
        removeCallbacks(requestScheduledFrame);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestRender();
    }

    private void scheduleNextFrame(final long delayNanos) {
        final long delayMillis = Math.max(1L, (delayNanos + 999_999L) / 1_000_000L);
        removeCallbacks(requestScheduledFrame);
        // Align the request with Android's display pulse after the engine's
        // absolute render deadline. This avoids millisecond timer drift in
        // long FMV and scrolling sequences.
        postOnAnimationDelayed(requestScheduledFrame, delayMillis);
    }

    public AndroidInputState inputState() {
        return input;
    }

    public void clearPressedInputs() {
        input.clearPressed();
    }

    public void attachPlatformManager(final AndroidPlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    public void requestImmersiveMode() {
        post(() -> setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE));
    }

    private final class GameRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
            final int[] major = new int[1];
            final int[] minor = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, major, 0);
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, minor, 0);
            if (major[0] < 3 || major[0] == 3 && minor[0] < 2) {
                throw new IllegalStateException("Severed Chains requires OpenGL ES 3.2; device provided "
                    + major[0] + "." + minor[0]);
            }
            GLES30.glClearColor(0.02f, 0.02f, 0.02f, 1.0f);
            surfaceReady = true;
            maybeStartGameEngine();
            // Each onDrawFrame call ends in eglSwapBuffers. Swapping continuously
            // while the engine intentionally renders at 15/20/30 FPS cycles stale
            // backbuffers on Mali, so request a surface frame only when an engine
            // action is due. This must happen after EGL initialization.
            post(() -> {
                setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                requestRender();
            });
            Log.i(TAG, "GLES surface created: version="
                + GLES30.glGetString(GLES30.GL_VERSION)
                + ", renderer=" + GLES30.glGetString(GLES30.GL_RENDERER)
                + ", extensions=" + GLES30.glGetString(GLES30.GL_EXTENSIONS));
        }

        @Override
        public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
            GLES30.glViewport(0, 0, width, height);
            final AndroidPlatformManager platform = platformManager;
            if (platform != null) platform.onSurfaceChanged(width, height);
            Log.i(TAG, "GLES surface size=" + width + "x" + height);
        }

        @Override
        public void onDrawFrame(final GL10 gl) {
            final AndroidPlatformManager platform = platformManager;
            if (platform != null) {
                final long nextFrameNanos = platform.dispatchFrame();
                scheduleNextFrame(nextFrameNanos);
                return;
            }
            // The real platform is attached by GameEngine startup. Until then,
            // keep the surface idle and input transitions frame-local.
            input.clearPressed();
            scheduleNextFrame(16_666_667L);
        }
    }

    private void maybeStartGameEngine() {
        if (!engineRequested || !surfaceReady || !engineStarted.compareAndSet(false, true)) return;
        engineThread = new Thread(() -> {
            try {
                Log.i(TAG, "Starting complete upstream GameEngine lifecycle");
                GameEngine.start();
            } catch (final Throwable throwable) {
                Log.e(TAG, "Upstream GameEngine failed", throwable);
            }
        }, "severed-chains-engine");
        engineThread.setDaemon(true);
        engineThread.start();
    }
}
