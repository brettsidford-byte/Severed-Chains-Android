package legend.severedchains.android;

import android.content.Context;

import java.io.File;

public final class AndroidStoragePaths {
    private final File root;

    public AndroidStoragePaths(final Context context) {
        root = context.getApplicationContext().getFilesDir();
    }

    public File root() {
        return root;
    }

    public File gameData() {
        return child("game-data");
    }

    public File extractedFiles() {
        return child("files");
    }

    public File saves() {
        return child("saves");
    }

    public File patches() {
        return child("patches");
    }

    public File mods() {
        return child("mods");
    }

    public File config() {
        return child("config.dcnf");
    }

    private File child(final String name) {
        return new File(root, name);
    }
}
