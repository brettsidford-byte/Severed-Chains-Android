package legend.severedchains.android;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class SeveredChainsSurfaceView extends GLSurfaceView {
    private static final String TAG = "SeveredChains";

    private final StatusRenderer renderer;
    private final AndroidInputState input = new AndroidInputState();

    public SeveredChainsSurfaceView(final Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setPreserveEGLContextOnPause(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        renderer = new StatusRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        requestFocus();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final String name = KeyEvent.keyCodeToString(keyCode);
        if (input.onKeyDown(keyCode, event)) {
            Log.i(TAG, "Controller key down: " + name);
            renderer.setLastInput("KEY " + name);
            return true;
        }
        Log.i(TAG, "Unmapped key down: " + name);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        final String name = KeyEvent.keyCodeToString(keyCode);
        if (input.onKeyUp(keyCode, event)) {
            Log.i(TAG, "Controller key up: " + name);
            renderer.setLastInput("KEY UP " + name);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (input.onGenericMotion(event)) {
            final String values = String.format(Locale.ROOT,
                "LX %.2f LY %.2f RX %.2f RY %.2f",
                input.axis(AndroidInputState.Control.LEFT_STICK_X),
                input.axis(AndroidInputState.Control.LEFT_STICK_Y),
                input.axis(AndroidInputState.Control.RIGHT_STICK_X),
                input.axis(AndroidInputState.Control.RIGHT_STICK_Y));
            Log.i(TAG, "Controller motion: " + values);
            renderer.setLastInput(values);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    public AndroidInputState inputState() {
        return input;
    }

    public void clearPressedInputs() {
        input.clearPressed();
    }

    private final class StatusRenderer implements GLSurfaceView.Renderer {
        private volatile String lastInput = "waiting for physical controller input";
        private int surfaceWidth;
        private int surfaceHeight;

        void setLastInput(final String value) {
            lastInput = value;
        }

        @Override
        public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
            GLES30.glClearColor(0.02f, 0.02f, 0.02f, 1.0f);
            Log.i(TAG, "GLES surface created: version="
                + GLES30.glGetString(GLES30.GL_VERSION)
                + ", renderer=" + GLES30.glGetString(GLES30.GL_RENDERER)
                + ", extensions=" + GLES30.glGetString(GLES30.GL_EXTENSIONS));
        }

        @Override
        public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
            surfaceWidth = width;
            surfaceHeight = height;
            GLES30.glViewport(0, 0, width, height);
            Log.i(TAG, "GLES surface size=" + width + "x" + height);
        }

        @Override
        public void onDrawFrame(final GL10 gl) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
            // Keep input transitions frame-local for the future engine adapter.
            input.clearPressed();
        }
    }
}
