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
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import legend.core.GamePaths;
import legend.core.audio.AudioBackends;
import legend.core.audio.AndroidAudioBackend;
import legend.core.audio.opus.AndroidFfmpegOpusDecoder;
import legend.core.audio.opus.AndroidMediaCodecOpusEncoder;
import legend.core.audio.opus.OpusDecoders;
import legend.core.audio.opus.OpusEncoders;
import legend.core.platform.AndroidPlatformManager;
import legend.core.platform.PlatformManagerFactory;
import legend.core.renderer.TextureBuilder;
import legend.game.textures.AndroidPngEncoder;
import legend.game.textures.PngWriter;

public final class MainActivity extends Activity {
    private static final String TAG = "SeveredChains";
    private static final String BUILD_LABEL = "Severed Chains Android 0.9.3";
    private static final int SELECT_GAME_DATA = 1001;

    private GameDataStore gameDataStore;
    private TextView status;
    private Button select;
    private SeveredChainsSurfaceView surface;
    private FrameLayout controls;
    private ImageView splash;

    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final AndroidStoragePaths storage = new AndroidStoragePaths(this);
        storage.ensureLayout();
        System.setProperty("severed.chains.root", storage.root().getAbsolutePath());
        GamePaths.configure(storage.root().toPath());
        try {
            BundledRuntimeAssets.install(this, GamePaths.root());
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to install bundled Severed Chains runtime assets", exception);
        }
        TextureBuilder.setPngDecoder(new AndroidTexturePngDecoder());
        PngWriter.setEncoder(new AndroidPngEncoder());
        AudioBackends.install(AndroidAudioBackend::new);
        OpusDecoders.install(AndroidFfmpegOpusDecoder::new);
        OpusEncoders.install(AndroidMediaCodecOpusEncoder::new);
        gameDataStore = new GameDataStore(this);
        Log.i(TAG, "Android engine host starting; storage root=" + storage.root()
            + ", shared GamePaths root=" + GamePaths.root()
            + ", isos=" + storage.isos()
            + ", extractedFiles=" + storage.extractedFiles());

        final FrameLayout root = new FrameLayout(this);
        surface = new SeveredChainsSurfaceView(this);
        PlatformManagerFactory.setFactory(() -> {
            final AndroidPlatformManager manager =
                new AndroidPlatformManager(this, surface, surface.inputState());
            surface.attachPlatformManager(manager);
            return manager;
        });
        root.addView(surface);

        controls = new FrameLayout(this);
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

        splash = new ImageView(this);
        splash.setBackgroundColor(0xff000000);
        splash.setImageResource(R.drawable.dragoon_icon_art);
        splash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(splash, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        hideSystemUi();
        if (gameDataStore.hasCompleteDiscSet()) {
            surface.startEngine();
            controls.setVisibility(android.view.View.GONE);
            dismissSplash(1_200L);
        } else {
            dismissSplash(500L);
        }
    }

    private void dismissSplash(final long delayMillis) {
        splash.animate()
            .alpha(0.0f)
            .setStartDelay(delayMillis)
            .setDuration(200L)
            .withEndAction(() -> {
                final android.view.ViewParent parent = splash.getParent();
                if(parent instanceof FrameLayout frameLayout) frameLayout.removeView(splash);
            })
            .start();
    }

    @Override
    public boolean dispatchKeyEvent(final android.view.KeyEvent event) {
        if (surface != null
            && (event.getSource() & android.view.InputDevice.SOURCE_GAMEPAD) != 0
            && (event.getSource() & android.view.InputDevice.SOURCE_KEYBOARD) == 0) {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                return surface.onKeyDown(event.getKeyCode(), event);
            }
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                return surface.onKeyUp(event.getKeyCode(), event);
            }
        }
        return super.dispatchKeyEvent(event);
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
                    final boolean ready = gameDataStore.hasCompleteDiscSet();
                    status.setText("ISO files copied to " + gameDataStore.getDataDirectory() + "\n"
                        + gameDataStore.getImportSummary() + "\n"
                        + GameDataInspector.inspect(gameDataStore.getImportedFiles())
                        + (ready ? "\nStarting Severed Chains..." : "\nSelect the missing or correct disc image(s)."));
                    if (ready) {
                        surface.startEngine();
                        controls.setVisibility(android.view.View.GONE);
                    }
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
            status.setText(BUILD_LABEL + "\n" + gameDataStore.getImportSummary() + "\n"
                + "ISO directory: " + gameDataStore.getDataDirectory() + "\n"
                + "Severed Chains startup will use these files\n"
                + GameDataInspector.inspect(gameDataStore.getImportedFiles()));
        } else {
            status.setText(BUILD_LABEL + "\nSelect the four ISO files\nOpenGL ES 3 surface active");
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

    @Override
    protected void onResume() {
        super.onResume();
        if (surface != null) surface.onResume();
        hideSystemUi();
    }

    @Override
    protected void onPause() {
        if (surface != null && isFinishing()) surface.shutdownEngine();
        if (surface != null) surface.onPause();
        super.onPause();
    }
}
