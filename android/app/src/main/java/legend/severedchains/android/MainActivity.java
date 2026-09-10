package legend.severedchains.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(createProbeView());
        hideSystemUi();
    }

    private View createProbeView() {
        final TextView view = new TextView(this);
        view.setText("Severed Chains Android\n\nAndroid packaging probe\nGame engine integration pending");
        view.setTextColor(0xffffffff);
        view.setTextSize(18);
        view.setGravity(android.view.Gravity.CENTER);
        view.setBackgroundColor(0xff000000);
        return view;
    }

    private void hideSystemUi() {
        final WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}
