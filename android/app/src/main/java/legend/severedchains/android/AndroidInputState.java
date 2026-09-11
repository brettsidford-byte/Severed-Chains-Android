package legend.severedchains.android;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Android-side controller state independent of the upstream desktop input
 * classes. It accepts the standard Android gamepad events used by handhelds
 * such as the RG405V and is ready to be translated to Severed Chains actions.
 */
public final class AndroidInputState {
    public enum Control {
        UP, DOWN, LEFT, RIGHT,
        A, B, X, Y,
        L1, R1, L2, R2,
        START, SELECT,
        LEFT_STICK_X, LEFT_STICK_Y,
        RIGHT_STICK_X, RIGHT_STICK_Y
    }

    private final boolean[] held = new boolean[Control.values().length];
    private final boolean[] pressed = new boolean[Control.values().length];
    private final float[] axes = new float[Control.values().length];

    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final Control control = controlForKey(keyCode);
        if (control == null) {
            return false;
        }

        final int index = control.ordinal();
        if (!held[index]) {
            pressed[index] = true;
        }
        held[index] = true;
        return true;
    }

    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        final Control control = controlForKey(keyCode);
        if (control == null) {
            return false;
        }

        held[control.ordinal()] = false;
        return true;
    }

    public boolean onGenericMotion(final MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == 0
            && (event.getSource() & InputDevice.SOURCE_GAMEPAD) == 0) {
            return false;
        }

        axes[Control.LEFT_STICK_X.ordinal()] = axis(event, MotionEvent.AXIS_X);
        axes[Control.LEFT_STICK_Y.ordinal()] = axis(event, MotionEvent.AXIS_Y);
        axes[Control.RIGHT_STICK_X.ordinal()] = axis(event, MotionEvent.AXIS_Z);
        axes[Control.RIGHT_STICK_Y.ordinal()] = axis(event, MotionEvent.AXIS_RZ);

        // Some Android drivers expose triggers as analogue axes rather than
        // buttons. Preserve both forms in the shared state.
        axes[Control.L2.ordinal()] = trigger(event, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE);
        axes[Control.R2.ordinal()] = trigger(event, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS);
        return true;
    }

    public boolean isHeld(final Control control) {
        return held[control.ordinal()];
    }

    public boolean wasPressed(final Control control) {
        return pressed[control.ordinal()];
    }

    public float axis(final Control control) {
        return axes[control.ordinal()];
    }

    public void clearPressed() {
        for (int i = 0; i < pressed.length; i++) {
            pressed[i] = false;
        }
    }

    private static float axis(final MotionEvent event, final int axis) {
        final InputDevice device = event.getDevice();
        if (device == null) {
            return 0.0f;
        }
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        final float value = event.getAxisValue(axis);
        if (range == null) {
            return value;
        }
        final float flat = range.getFlat();
        return Math.abs(value) < flat ? 0.0f : value;
    }

    private static float trigger(final MotionEvent event, final int primary, final int fallback) {
        final float value = axis(event, primary);
        return value != 0.0f ? value : axis(event, fallback);
    }

    private static Control controlForKey(final int keyCode) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> Control.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN -> Control.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT -> Control.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT -> Control.RIGHT;
            case KeyEvent.KEYCODE_BUTTON_A -> Control.A;
            case KeyEvent.KEYCODE_BUTTON_B -> Control.B;
            case KeyEvent.KEYCODE_BUTTON_X -> Control.X;
            case KeyEvent.KEYCODE_BUTTON_Y -> Control.Y;
            case KeyEvent.KEYCODE_BUTTON_L1 -> Control.L1;
            case KeyEvent.KEYCODE_BUTTON_R1 -> Control.R1;
            case KeyEvent.KEYCODE_BUTTON_L2 -> Control.L2;
            case KeyEvent.KEYCODE_BUTTON_R2 -> Control.R2;
            case KeyEvent.KEYCODE_BUTTON_START -> Control.START;
            case KeyEvent.KEYCODE_BUTTON_SELECT -> Control.SELECT;
            default -> null;
        };
    }
}
