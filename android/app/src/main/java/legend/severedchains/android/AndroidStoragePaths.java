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

    public void ensureLayout() {
        root.mkdirs();
        isos().mkdirs();
        extractedFiles().mkdirs();
        saves().mkdirs();
        patches().mkdirs();
        mods().mkdirs();
    }

    public File isos() {
        return child("isos");
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
