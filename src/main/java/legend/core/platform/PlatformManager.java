package legend.core.platform;

import legend.core.DebugHelper;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputAxis;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputGamepadType;
import legend.core.platform.input.InputKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PlatformManager {
  private final List<Window> windows = new ArrayList<>();
  private final List<Action> actions = new ArrayList<>();
  private boolean running;
  private boolean defaultFullscreen;

  private final Action input = this.addAction(new Action(this::tickInput, 60));;

  public abstract void init();

  /** Event-driven platforms may prepare blocking FMV demux/decode work off their render thread. */
  public boolean preparesFmvAsynchronously() { return false; }

  public void destroy() {
    this.running = false;
  }

  public abstract boolean isContextCurrent();

  public abstract boolean hasGamepad();
  public abstract InputGamepadType getGamepadType();
  public abstract int getMouseButton(final int index);

  public abstract String[] listDisplays();

  protected abstract Window createWindow(final String title, final int width, final int height);

  public Window addWindow(final String name, final int width, final int height) {
    final Window window = this.createWindow(name, width, height);
    this.windows.add(window);

    if(this.defaultFullscreen) {
      window.makeFullscreen();
    } else {
      window.centerWindow();
    }

    return window;
  }

  public void setDefaultFullscreen(final boolean fullscreen) {
    this.defaultFullscreen = fullscreen;
  }

  protected void removeWindows(final Collection<Window> windows) {
    this.windows.removeAll(windows);
  }

  public abstract Window getLastWindow();

  public synchronized Action addAction(final Action action) {
    this.actions.add(action);
    return action;
  }

  public synchronized void removeAction(final Action action) {
    this.actions.remove(action);
  }

  public void stop() {
    this.running = false;
  }

  public void run() {
    this.running = true;

    final Set<Window> toRemove = new HashSet<>();

    while(this.running) {
      for(int i = 0; i < this.windows.size(); i++) {
        final Window window = this.windows.get(i);

        if(window.shouldClose()) {
          window.destroy();
          toRemove.add(window);
        }
      }

      this.removeWindows(toRemove);

      final Action nextAction = this.tickActions();

      DebugHelper.sleep(Math.max(0, nextAction.nanosUntilNextRun() / 1_000_000));
    }
  }

  /** Runs scheduled platform actions once; event-driven platforms call this from their frame callback. */
  protected final synchronized Action tickActions() {
    Action nextAction = null;
    for(int i = 0; i < this.actions.size(); i++) {
      final Action action = this.actions.get(i);
      action.tick();
      if(nextAction == null || action.nanosUntilNextRun() < nextAction.nanosUntilNextRun()) {
        nextAction = action;
      }
    }
    return nextAction;
  }

  public void setInputTickRate(final int hz) {
    this.input.setExpectedFps(hz);
  }

  public void resetActionStates() {

  }

  /** Called at the end of each game tick to clear one-frame input */
  public void clearPressed() {

  }

  protected abstract void tickInput();
  public abstract void rumble(final float intensity, final int ms);
  public abstract void rumble(final float bigIntensity, final float smallIntensity, final int ms);
  public abstract void adjustRumble(final float intensity, final int ms);
  public abstract void adjustRumble(final float bigIntensity, final float smallIntensity, final int ms);
  public abstract void stopRumble();

  public abstract String getKeyName(final InputKey key);
  public abstract String getScancodeName(final InputKey key);
  public abstract String getButtonName(final InputButton button);
  public abstract String getAxisName(final InputAxis axis);

  public abstract boolean isActionPressed(final InputAction action);
  public abstract boolean isActionRepeat(final InputAction action);
  public abstract boolean isActionHeld(final InputAction action);
  public abstract float getAxis(final InputAction action);

  public abstract void openUrl(final String url);

  /** Requests that the host close the application. */
  public void requestExit() { }

  /** Opens desktop debugging tools when the host provides them. */
  public void openDebugger() { }

  /** Runs work that creates or destroys GPU resources on the host render-context thread. */
  public void runOnRenderThread(final Runnable action) {
    action.run();
  }

  /** Android/Dex hosts explicitly register built-in mods and listeners. */
  public boolean usesBundledModDiscovery() { return false; }

  /** Whether this host can consume the desktop release/self-update channel. */
  public boolean supportsUpdateChecks() { return true; }
}
