package legend.severedchains.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final FrameLayout root = new FrameLayout(this);
        root.addView(new SeveredChainsSurfaceView(this));

        final TextView status = new TextView(this);
        status.setText("Severed Chains Android\nOpenGL ES 3 surface active\nPhysical controls are being routed");
        status.setTextColor(0xffffffff);
        status.setTextSize(16);
        status.setGravity(android.view.Gravity.CENTER);
        root.addView(status, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        hideSystemUi();
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
