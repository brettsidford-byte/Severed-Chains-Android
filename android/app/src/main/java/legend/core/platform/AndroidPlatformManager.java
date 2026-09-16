package legend.core.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.EGL14;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;

import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputAxis;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputGamepadType;
import legend.core.platform.input.InputKey;
import legend.core.platform.input.InputActionState;
import legend.core.platform.input.InputBinding;
import legend.core.platform.input.InputBindings;
import legend.core.platform.input.ButtonInputActivation;
import legend.core.platform.input.AxisInputActivation;
import legend.core.platform.input.InputAxisDirection;
import legend.severedchains.android.AndroidInputState;
import legend.severedchains.android.SeveredChainsSurfaceView;

import static legend.core.GameEngine.CONFIG;
import static legend.game.modding.coremod.CoreMod.MENU_INNER_DEADZONE_CONFIG;
import static legend.game.modding.coremod.CoreMod.MENU_OUTER_DEADZONE_CONFIG;
import static legend.game.modding.coremod.CoreMod.MOVEMENT_INNER_DEADZONE_CONFIG;
import static legend.game.modding.coremod.CoreMod.MOVEMENT_OUTER_DEADZONE_CONFIG;

/** Android implementation of the shared platform lifecycle and action-state contract. */
public final class AndroidPlatformManager extends PlatformManager {
    private final Context context;
    private final Activity activity;
    private final SeveredChainsSurfaceView surface;
    private final AndroidInputState input;
    private final Object runLock = new Object();
    private final Map<InputAction, InputActionState> actionStates = new IdentityHashMap<>();
    private final Set<InputAction> pressed = new HashSet<>();
    private AndroidWindow lastWindow;
    private volatile boolean running;
    private boolean stopRequested;

    public AndroidPlatformManager(final Context context,
                                  final SeveredChainsSurfaceView surface,
                                  final AndroidInputState input) {
        this.context = context.getApplicationContext();
        this.activity = (Activity) context;
        this.surface = surface;
        this.input = input;
    }

    @Override public void init() { }
    @Override public boolean preparesFmvAsynchronously() { return true; }
    @Override public boolean usesBundledModDiscovery() { return true; }
    @Override public boolean supportsUpdateChecks() { return false; }

    @Override
    public void runOnRenderThread(final Runnable action) {
        if (isContextCurrent()) {
            action.run();
            return;
        }
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        surface.queueEvent(() -> {
            try {
                action.run();
            } catch (final Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });
        try {
            finished.await();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the Android render thread", exception);
        }
        if (failure.get() != null) {
            if (failure.get() instanceof RuntimeException runtime) throw runtime;
            if (failure.get() instanceof Error error) throw error;
            throw new IllegalStateException("Android render-thread operation failed", failure.get());
        }
    }

    @Override
    public void destroy() {
        stopRumble();
        stop();
        super.destroy();
    }

    @Override
    public boolean isContextCurrent() {
        return EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT;
    }

    @Override
    public boolean hasGamepad() {
        for (final int id : InputDevice.getDeviceIds()) {
            final InputDevice device = InputDevice.getDevice(id);
            if (device != null && ((device.getSources() & InputDevice.SOURCE_GAMEPAD) != 0
                || (device.getSources() & InputDevice.SOURCE_JOYSTICK) != 0)) return true;
        }
        return false;
    }

    @Override public InputGamepadType getGamepadType() { return InputGamepadType.STANDARD; }
    @Override public int getMouseButton(final int index) { return index; }
    @Override public String[] listDisplays() { return new String[]{"Android display"}; }

    @Override
    protected Window createWindow(final String title, final int width, final int height) {
        lastWindow = new AndroidWindow(this, surface, title, width, height);
        return lastWindow;
    }

    @Override public Window getLastWindow() { return lastWindow; }

    /**
     * Called by GLSurfaceView on the thread owning the EGL context.
     *
     * @return nanoseconds until the next scheduled platform action
     */
    public long dispatchFrame() {
        // The surface can begin producing frames while GameEngine is still
        // registering input bindings and render actions on its startup thread.
        // Do not observe those mutable registries until PlatformManager.run()
        // marks initialization complete.
        if (!running) return 16_666_667L;
        tickActions();

        final AndroidWindow closingWindow = lastWindow;
        if (closingWindow != null && closingWindow.shouldClose()) {
            closingWindow.destroy();
            removeWindows(List.of(closingWindow));
            lastWindow = null;
            stop();
            activity.runOnUiThread(activity::finishAndRemoveTask);
            return 16_666_667L;
        }

        // The 60 Hz input action must not drive GLSurfaceView presentation.
        // Every onDrawFrame callback is followed by eglSwapBuffers, even when
        // the render action was not due, so using the earliest platform action
        // here presents duplicate/stale buffers between 15/20/30 FPS frames.
        // Schedule the next surface callback from the render action itself.
        final AndroidWindow window = lastWindow;
        return window == null
            ? 16_666_667L
            : Math.max(0L, window.nanosUntilNextRender());
    }

    public void onSurfaceChanged(final int width, final int height) {
        if (lastWindow != null) lastWindow.surfaceChanged(width, height);
    }

    public void dispatchButtonPressed(final InputButton button, final boolean repeat) {
        if(lastWindow != null) lastWindow.events().onButtonPress(button, repeat);
    }

    public void dispatchButtonReleased(final InputButton button) {
        if(lastWindow != null) lastWindow.events().onButtonRelease(button);
    }

    /** Dispatches the raw controller axis event used by the rebinding UI. */
    public void dispatchAxis(final InputAxis axis, final float rawValue) {
        if(lastWindow == null || rawValue == 0.0f) return;

        final float menuValue = applyDeadzone(rawValue,
            CONFIG.getConfig(MENU_INNER_DEADZONE_CONFIG.get()),
            CONFIG.getConfig(MENU_OUTER_DEADZONE_CONFIG.get()));
        final float movementValue = applyDeadzone(rawValue,
            CONFIG.getConfig(MOVEMENT_INNER_DEADZONE_CONFIG.get()),
            CONFIG.getConfig(MOVEMENT_OUTER_DEADZONE_CONFIG.get()));
        if(menuValue == 0.0f && movementValue == 0.0f) return;

        final InputAxisDirection direction = rawValue < 0.0f
            ? InputAxisDirection.NEGATIVE : InputAxisDirection.POSITIVE;
        lastWindow.events().onAxis(axis, direction, menuValue, movementValue);
    }

    @Override
    public void run() {
        synchronized (runLock) {
            running = !stopRequested;
            while (running) {
                try {
                    runLock.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    @Override
    public void stop() {
        synchronized (runLock) {
            stopRequested = true;
            running = false;
            runLock.notifyAll();
        }
        super.stop();
    }

    @Override
    protected void tickInput() {
        final Map<InputAction, Float> active = new IdentityHashMap<>();
        final List<InputBinding<ButtonInputActivation>> buttons =
            InputBindings.getBindings(ButtonInputActivation.class);
        for (final InputBinding<ButtonInputActivation> binding : buttons) {
            // SDL button bindings hold the action but do not provide an
            // analogue value. Keeping this at zero is important: EngineState
            // combines signed directional axes as left+right and up+down.
            if (input.isHeld(binding.activation.button)) active.putIfAbsent(binding.action, 0.0f);
        }
        final List<InputBinding<AxisInputActivation>> axes =
            InputBindings.getBindings(AxisInputActivation.class);
        for (final InputBinding<AxisInputActivation> binding : axes) {
            final float raw = input.axis(binding.activation.axis);
            final InputAxisDirection direction = raw < 0.0f
                ? InputAxisDirection.NEGATIVE : InputAxisDirection.POSITIVE;
            if(binding.activation.direction != direction) continue;

            final float inner = CONFIG.getConfig((binding.action.useMovementDeadzone
                ? MOVEMENT_INNER_DEADZONE_CONFIG : MENU_INNER_DEADZONE_CONFIG).get());
            final float outer = CONFIG.getConfig((binding.action.useMovementDeadzone
                ? MOVEMENT_OUTER_DEADZONE_CONFIG : MENU_OUTER_DEADZONE_CONFIG).get());
            final float magnitude = applyDeadzone(raw, inner, outer);
            if(magnitude > 0.0f) {
                // Preserve the physical sign exactly as the desktop SDL path
                // does. Negative X/Y represents left/up; positive is right/down.
                final float signedValue = Math.copySign(magnitude, raw);
                active.merge(binding.action, signedValue,
                    (first, second) -> Math.abs(second) > Math.abs(first) ? second : first);
            }
        }

        for (final Map.Entry<InputAction, Float> entry : active.entrySet()) {
            final InputActionState state = actionStates.computeIfAbsent(entry.getKey(), ignored -> new InputActionState());
            if (!state.isHeld()) {
                state.press();
                pressed.add(entry.getKey());
                if (lastWindow != null) lastWindow.events().onInputActionPressed(entry.getKey(), false);
            }
            state.axis(entry.getValue());
        }
        for (final Map.Entry<InputAction, InputActionState> entry : actionStates.entrySet()) {
            if (!active.containsKey(entry.getKey()) && entry.getValue().isHeld()) {
                entry.getValue().release();
                if (lastWindow != null) lastWindow.events().onInputActionReleased(entry.getKey());
            } else if (active.containsKey(entry.getKey()) && entry.getValue().repeat()) {
                if (lastWindow != null) lastWindow.events().onInputActionPressed(entry.getKey(), true);
            }
        }
    }

    public void setActionState(final InputAction action, final boolean isHeld,
                               final boolean wasPressed, final boolean isRepeat,
                               final float axisValue) {
        if (wasPressed) pressed.add(action); else pressed.remove(action);
        final InputActionState state = actionStates.computeIfAbsent(action, ignored -> new InputActionState());
        if (isHeld && !state.isHeld()) state.press();
        if (!isHeld) state.release();
        if (axisValue != 0.0f) state.axis(axisValue);
    }

    @Override
    public void resetActionStates() {
        pressed.clear();
        actionStates.clear();
    }

    @Override
    public void clearPressed() {
        pressed.clear();
        input.clearPressed();
    }

    @Override public boolean isActionPressed(final InputAction action) { return pressed.contains(action); }
    @Override public boolean isActionRepeat(final InputAction action) { return state(action).isRepeat(); }
    @Override public boolean isActionHeld(final InputAction action) { return state(action).isHeld(); }
    @Override public float getAxis(final InputAction action) { return state(action).getAxis(); }

    private InputActionState state(final InputAction action) {
        return actionStates.computeIfAbsent(action, ignored -> new InputActionState());
    }

    private static float applyDeadzone(final float value, final float inner, final float outer) {
        final float magnitude = Math.abs(value);
        if(magnitude <= inner) return 0.0f;
        if(outer <= inner) return 1.0f;
        return Math.min(1.0f, Math.max(0.0f, (magnitude - inner) / (outer - inner)));
    }

    @Override public String getKeyName(final InputKey key) { return key.name(); }
    @Override public String getScancodeName(final InputKey key) { return key.name(); }
    @Override public String getButtonName(final InputButton button) { return button.name(); }
    @Override public String getAxisName(final InputAxis axis) { return axis.name(); }

    @Override public void rumble(final float intensity, final int ms) { rumble(intensity, intensity, ms); }

    @Override
    public void rumble(final float bigIntensity, final float smallIntensity, final int ms) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator() || ms <= 0) return;
        final float intensity = Math.max(bigIntensity, smallIntensity);
        final int amplitude = Math.max(1, Math.min(255, Math.round(intensity * 255.0f)));
        vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
    }

    @Override public void adjustRumble(final float intensity, final int ms) { rumble(intensity, ms); }
    @Override public void adjustRumble(final float bigIntensity, final float smallIntensity, final int ms) { rumble(bigIntensity, smallIntensity, ms); }

    @Override
    public void stopRumble() {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    public void openUrl(final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void requestExit() {
        activity.runOnUiThread(activity::finish);
    }
}
