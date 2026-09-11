package legend.severedchains.android;

import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;

import legend.core.GamePaths;

/**
 * Android-side hand-off between game-data preparation and the future native engine host.
 * This deliberately does not duplicate engine logic: it validates the shared output paths
 * and gives the Activity a single lifecycle boundary to replace the diagnostic surface.
 */
public final class AndroidEngineSession {
    private static final String TAG = "SeveredChains";

    private AndroidEngineSession() {
    }

    public static boolean isGameDataReady() {
        final Path files = GamePaths.files();
        final Path version = files.resolve("version");
        final boolean ready = Files.isDirectory(files) && Files.isRegularFile(version);
        Log.i(TAG, "Engine data ready=" + ready + ", files=" + files);
        return ready;
    }

    public static String describe() {
        if (isGameDataReady()) {
            return "Severed Chains data is ready for the Android engine";
        }
        return "Severed Chains data is not yet unpacked";
    }
}
