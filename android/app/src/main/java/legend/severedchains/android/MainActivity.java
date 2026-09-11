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

public final class MainActivity extends Activity {
    private static final String TAG = "SeveredChains";
    private static final int SELECT_GAME_DATA = 1001;

    private GameDataStore gameDataStore;
    private TextView status;
    private Button select;
    private Button extract;

    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        gameDataStore = new GameDataStore(this);
        Log.i(TAG, "Android probe starting; storage root=" + new AndroidStoragePaths(this).root());

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

        extract = new Button(this);
        extract.setText("Extract imported game data");
        extract.setOnClickListener(view -> extractGameData());
        final FrameLayout.LayoutParams extractParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        extractParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        extractParams.bottomMargin = 72;
        controls.addView(extract, extractParams);

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
        if (requestCode != SELECT_GAME_DATA || resultCode != RESULT_OK || data == null) {
            return;
        }
        status.setText("Copying game-data into Android storage...\\nPlease keep the app open");
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
                    extract.setEnabled(true);
                });
            } catch (final IOException exception) {
                Log.e(TAG, "Game-data import failed", exception);
                runOnUiThread(() -> {
                    status.setText("Game-data import failed: " + exception.getMessage());
                    select.setEnabled(true);
                    extract.setEnabled(gameDataStore.hasImportedData());
                });
            }
        }, "game-data-import").start();
    }

    private void extractGameData() {
        status.setText("Extracting PlayStation ISO files...\\nPlease keep the app open");
        select.setEnabled(false);
        extract.setEnabled(false);
        new Thread(() -> {
            try {
                AndroidIsoExtractor.extract(gameDataStore.getImportedFiles(),
                    new AndroidStoragePaths(this).extractedFiles(),
                    message -> runOnUiThread(() -> status.setText(message)));
                runOnUiThread(() -> {
                    status.setText("ISO extraction complete\\n"
                        + "Output: " + new AndroidStoragePaths(this).extractedFiles());
                    select.setEnabled(true);
                    extract.setEnabled(true);
                });
            } catch (final IOException exception) {
                Log.e(TAG, "ISO extraction failed", exception);
                runOnUiThread(() -> {
                    status.setText("ISO extraction failed: " + exception.getMessage());
                    select.setEnabled(true);
                    extract.setEnabled(true);
                });
            }
        }, "iso-extraction").start();
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
