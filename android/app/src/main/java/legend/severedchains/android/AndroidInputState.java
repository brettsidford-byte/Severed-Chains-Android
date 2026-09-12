package legend.severedchains.android;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import legend.core.platform.input.InputAxis;
import legend.core.platform.input.InputButton;

/**
 * Android-side controller state independent of the upstream desktop input
 * event loop. It accepts the standard Android gamepad events used by handhelds
 * such as the RG405V and stores them using the upstream controller enums so the
 * platform manager can apply the existing user-configurable action bindings.
 */
public final class AndroidInputState {
    private final boolean[] held = new boolean[InputButton.values().length];
    private final boolean[] pressed = new boolean[InputButton.values().length];
    private final float[] axes = new float[InputAxis.values().length];

    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            axes[InputAxis.LEFT_TRIGGER.ordinal()] = 1.0f;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            axes[InputAxis.RIGHT_TRIGGER.ordinal()] = 1.0f;
            return true;
        }
        final InputButton button = buttonForKey(keyCode);
        if (button == null) {
            return false;
        }

        final int index = button.ordinal();
        if (!held[index]) {
            pressed[index] = true;
        }
        held[index] = true;
        return true;
    }

    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            axes[InputAxis.LEFT_TRIGGER.ordinal()] = 0.0f;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            axes[InputAxis.RIGHT_TRIGGER.ordinal()] = 0.0f;
            return true;
        }
        final InputButton button = buttonForKey(keyCode);
        if (button == null) {
            return false;
        }

        held[button.ordinal()] = false;
        return true;
    }

    public boolean onGenericMotion(final MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == 0
            && (event.getSource() & InputDevice.SOURCE_GAMEPAD) == 0) {
            return false;
        }

        axes[InputAxis.LEFT_X.ordinal()] = axis(event, MotionEvent.AXIS_X);
        axes[InputAxis.LEFT_Y.ordinal()] = axis(event, MotionEvent.AXIS_Y);
        axes[InputAxis.RIGHT_X.ordinal()] = axis(event, MotionEvent.AXIS_Z);
        axes[InputAxis.RIGHT_Y.ordinal()] = axis(event, MotionEvent.AXIS_RZ);

        // Some Android drivers expose triggers as analogue axes rather than
        // buttons. Preserve both forms in the shared state.
        axes[InputAxis.LEFT_TRIGGER.ordinal()] = trigger(event, MotionEvent.AXIS_LTRIGGER,
            MotionEvent.AXIS_BRAKE);
        axes[InputAxis.RIGHT_TRIGGER.ordinal()] = trigger(event, MotionEvent.AXIS_RTRIGGER,
            MotionEvent.AXIS_GAS);
        return true;
    }

    public boolean isHeld(final InputButton button) {
        return held[button.ordinal()];
    }

    public boolean wasPressed(final InputButton button) {
        return pressed[button.ordinal()];
    }

    public float axis(final InputAxis axis) {
        return axes[axis.ordinal()];
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

    private static InputButton buttonForKey(final int keyCode) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> InputButton.DPAD_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN -> InputButton.DPAD_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT -> InputButton.DPAD_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT -> InputButton.DPAD_RIGHT;
            case KeyEvent.KEYCODE_BUTTON_A -> InputButton.A;
            case KeyEvent.KEYCODE_BUTTON_B -> InputButton.B;
            case KeyEvent.KEYCODE_BUTTON_X -> InputButton.X;
            case KeyEvent.KEYCODE_BUTTON_Y -> InputButton.Y;
            case KeyEvent.KEYCODE_BUTTON_L1 -> InputButton.LEFT_BUMPER;
            case KeyEvent.KEYCODE_BUTTON_R1 -> InputButton.RIGHT_BUMPER;
            case KeyEvent.KEYCODE_BUTTON_THUMBL -> InputButton.LEFT_STICK;
            case KeyEvent.KEYCODE_BUTTON_THUMBR -> InputButton.RIGHT_STICK;
            case KeyEvent.KEYCODE_BUTTON_START -> InputButton.START;
            case KeyEvent.KEYCODE_BUTTON_SELECT -> InputButton.SELECT;
            default -> null;
        };
    }
}
