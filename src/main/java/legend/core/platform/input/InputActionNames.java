package legend.core.platform.input;

import java.util.List;

import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.RENDERER;

/** Resolves configured action bindings into the label shown by the game UI. */
public final class InputActionNames {
  private InputActionNames() { }

  public static String getActionName(final InputAction action) {
    final InputClass type = RENDERER.window().getInputClass();
    final List<InputActivation> activations = InputBindings.getActivationsForAction(action);

    for(int i = 0; i < activations.size(); i++) {
      final InputActivation activation = activations.get(i);

      if(type == InputClass.GAMEPAD) {
        if(activation instanceof final ButtonInputActivation button) {
          return String.valueOf(button.button.codepoint);
        }

        if(activation instanceof final AxisInputActivation axis) {
          return String.valueOf(axis.axis.codepoint);
        }
      }

      if(type == InputClass.KEYBOARD) {
        if(activation instanceof final KeyInputActivation key) {
          return PLATFORM.getKeyName(key.key);
        }

        if(activation instanceof final ScancodeInputActivation scancode) {
          return PLATFORM.getKeyName(scancode.key);
        }
      }
    }

    return "<unbound>";
  }
}
