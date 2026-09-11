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

    public SeveredChainsSurfaceView(final Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setFocusable(true);
        setFocusableInTouchMode(true);
        renderer = new StatusRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final String input = String.format(Locale.ROOT, "KEY %s", KeyEvent.keyCodeToString(keyCode));
        Log.i(TAG, input);
        renderer.setLastInput(input);
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        final String input = String.format(Locale.ROOT, "KEY UP %s", KeyEvent.keyCodeToString(keyCode));
        Log.i(TAG, input);
        renderer.setLastInput(input);
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) != 0) {
            final float x = event.getAxisValue(MotionEvent.AXIS_X);
            final float y = event.getAxisValue(MotionEvent.AXIS_Y);
            final String input = String.format(Locale.ROOT, "STICK %.2f, %.2f", x, y);
            Log.i(TAG, input);
            renderer.setLastInput(input);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private static final class StatusRenderer implements GLSurfaceView.Renderer {
        private volatile String lastInput = "waiting for physical controller input";

        void setLastInput(final String input) {
            lastInput = input;
        }

        @Override
        public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
            GLES30.glClearColor(0.02f, 0.02f, 0.02f, 1.0f);
            Log.i(TAG, "GLES surface created: version=" + GLES30.glGetString(GLES30.GL_VERSION) + ", renderer=" + GLES30.glGetString(GLES30.GL_RENDERER));
        }

        @Override
        public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
            GLES30.glViewport(0, 0, width, height);
            Log.i(TAG, "GLES surface size=" + width + "x" + height);
        }

        @Override
        public void onDrawFrame(final GL10 gl) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }
}
