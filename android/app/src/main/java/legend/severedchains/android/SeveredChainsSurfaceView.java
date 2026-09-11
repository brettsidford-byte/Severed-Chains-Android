package legend.severedchains.android;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class SeveredChainsSurfaceView extends GLSurfaceView {
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
        renderer.setLastInput(String.format(Locale.ROOT, "KEY %s", KeyEvent.keyCodeToString(keyCode)));
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        renderer.setLastInput(String.format(Locale.ROOT, "KEY UP %s", KeyEvent.keyCodeToString(keyCode)));
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) != 0) {
            final float x = event.getAxisValue(MotionEvent.AXIS_X);
            final float y = event.getAxisValue(MotionEvent.AXIS_Y);
            renderer.setLastInput(String.format(Locale.ROOT, "STICK %.2f, %.2f", x, y));
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
        }

        @Override
        public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
            GLES30.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(final GL10 gl) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }
}
