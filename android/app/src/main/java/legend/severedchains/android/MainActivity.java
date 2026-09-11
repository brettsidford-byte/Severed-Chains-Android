package legend.severedchains.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import legend.core.GamePaths;

public final class MainActivity extends Activity {
    private static final String TAG = "SeveredChains";
    private static final int SELECT_GAME_DATA = 1001;

    private GameDataStore gameDataStore;
    private TextView status;
    private Button select;

    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final AndroidStoragePaths storage = new AndroidStoragePaths(this);
        System.setProperty("severed.chains.root", storage.root().getAbsolutePath());
        GamePaths.configure(storage.root().toPath());
        gameDataStore = new GameDataStore(this);
        Log.i(TAG, "Android probe starting; storage root=" + storage.root()
            + ", shared GamePaths root=" + GamePaths.root());

        final FrameLayout root = new FrameLayout(this);
        root.addView(new SeveredChainsSurfaceView(this));

        final FrameLayout controls = new FrameLayout(this);
        status = new TextView(this);
        status.setTextColor(0xffffffff);
        status.setTextSize(16);
        status.setGravity(android.view.Gravity.CENTER);
        updateStatus();
        controls.addView(status, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        select = new Button(this);
        select.setText("Select all four ISO files");
        select.setOnClickListener(view -> selectGameData());
        final FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        controls.addView(select, buttonParams);

        root.addView(controls, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        hideSystemUi();
    }

    private void selectGameData() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, SELECT_GAME_DATA);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SELECT_GAME_DATA || resultCode != RESULT_OK || data == null) return;

        status.setText("Copying game-data into Android isos storage...\nPlease keep the app open");
        select.setEnabled(false);
        new Thread(() -> {
            try {
                final List<java.io.File> imported = gameDataStore.importDocuments(data);
                Log.i(TAG, "Imported " + imported.size() + " selected file(s); total available="
                    + gameDataStore.getImportedFileCount());
                runOnUiThread(() -> {
                    status.setText(gameDataStore.getImportSummary() + "\n"
                        + GameDataInspector.inspect(gameDataStore.getImportedFiles()));
                    select.setEnabled(true);
                });
            } catch (final IOException exception) {
                Log.e(TAG, "Game-data import failed", exception);
                runOnUiThread(() -> {
                    status.setText("Game-data import failed: " + exception.getMessage());
                    select.setEnabled(true);
                });
            }
        }, "game-data-import").start();
    }

    private void updateStatus() {
        final int count = gameDataStore.getImportedFileCount();
        if (count > 0) {
            status.setText(gameDataStore.getImportSummary() + "\n"
                + GameDataInspector.inspect(gameDataStore.getImportedFiles())
                + "\nOpenGL ES 3 surface active");
        } else {
            status.setText("Select all four ISO files\nOpenGL ES 3 surface active");
        }
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
