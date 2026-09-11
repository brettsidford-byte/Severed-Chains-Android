package legend.severedchains.android;

import android.util.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import legend.core.GamePaths;

/**
 * Android-side hand-off between ISO input, the upstream data preparation path,
 * and the future native renderer/game loop.
 */
public final class AndroidEngineSession {
    private static final String TAG = "SeveredChains";

    private AndroidEngineSession() {
    }

    public static boolean hasIsoInput() {
        final File directory = GamePaths.isos().toFile();
        final File[] files = directory.listFiles(file -> file.isFile());
        final boolean ready = files != null && files.length >= 4;
        Log.i(TAG, "ISO input ready=" + ready + ", directory=" + directory
            + ", fileCount=" + (files == null ? 0 : files.length));
        return ready;
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
        if (hasIsoInput()) {
            return "Severed Chains ISO input is ready; startup preparation will run";
        }
        return "Select the four Severed Chains ISO files";
    }
}
