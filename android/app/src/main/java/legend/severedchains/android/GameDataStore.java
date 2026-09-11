package legend.severedchains.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class GameDataStore {
    private static final String DATA_DIRECTORY = "game-data";
    private static final String IMPORTED_FILES = "imported-files.txt";

    private final Context context;

    public GameDataStore(final Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasImportedData() {
        final File directory = new File(context.getFilesDir(), DATA_DIRECTORY);
        final File manifest = new File(directory, IMPORTED_FILES);
        return manifest.isFile() && manifest.length() > 0L;
    }

    public List<File> importDocuments(final Intent result) throws IOException {
        final List<File> imported = new ArrayList<>();
        final File directory = new File(context.getFilesDir(), DATA_DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create Android game-data directory");
        }

        final List<Uri> sources = new ArrayList<>();
        if (result.getClipData() != null) {
            for (int i = 0; i < result.getClipData().getItemCount(); i++) {
                sources.add(result.getClipData().getItemAt(i).getUri());
            }
        } else if (result.getData() != null) {
            sources.add(result.getData());
        }

        for (final Uri source : sources) {
            final String name = safeName(source);
            final File destination = new File(directory, name);
            copy(source, destination);
            imported.add(destination);
        }

        final File manifest = new File(directory, IMPORTED_FILES);
        try (FileOutputStream output = new FileOutputStream(manifest, false)) {
            for (final File file : imported) {
                output.write((file.getName() + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return imported;
    }

    public File getDataDirectory() {
        return new File(context.getFilesDir(), DATA_DIRECTORY);
    }

    private void copy(final Uri source, final File destination) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(destination, false)) {
            if (input == null) {
                throw new IOException("Unable to open selected game-data file");
            }
            final byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    output.write(buffer, 0, count);
                }
            }
        }
    }

    private String safeName(final Uri source) {
        final String raw = source.getLastPathSegment();
        final String name = raw == null ? "game-data.bin" : raw.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isEmpty() ? "game-data.bin" : name;
    }
}
