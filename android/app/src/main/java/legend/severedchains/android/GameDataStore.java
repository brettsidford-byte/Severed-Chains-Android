package legend.severedchains.android;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GameDataStore {
    private static final String IMPORTED_FILES = "imported-files.txt";

    private final Context context;
    private final AndroidStoragePaths paths;

    public GameDataStore(final Context context) {
        this.context = context.getApplicationContext();
        paths = new AndroidStoragePaths(this.context);
        migrateLegacyData();
    }

    public boolean hasImportedData() {
        return getImportedFileCount() > 0;
    }

    public String getImportSummary() {
        final List<File> files = listedFiles();
        if (files.isEmpty()) return "No game-data files imported";
        long bytes = 0;
        final StringBuilder summary = new StringBuilder(files.size() + " game-data file(s) available\n");
        for (final File file : files) {
            bytes += file.length();
            summary.append(file.getName()).append("\n");
        }
        summary.append(formatBytes(bytes)).append(" total");
        return summary.toString();
    }

    public int getImportedFileCount() {
        return listedFiles().size();
    }

    public List<File> importDocuments(final Intent result) throws IOException {
        final List<File> imported = new ArrayList<>();
        final File directory = paths.isos();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create Android isos directory");
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
            final File destination = new File(directory, safeName(source));
            copy(source, destination);
            imported.add(destination);
        }

        final Set<String> allFiles = new LinkedHashSet<>();
        final File manifest = new File(directory, IMPORTED_FILES);
        if (manifest.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(manifest))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) allFiles.add(line);
                }
            }
        }
        for (final File file : imported) allFiles.add(file.getName());
        writeManifest(manifest, allFiles);
        return imported;
    }

    public List<File> getImportedFiles() {
        return listedFiles();
    }

    public File getDataDirectory() {
        return paths.isos();
    }

    private void migrateLegacyData() {
        final File legacy = paths.gameData();
        final File oldManifest = new File(legacy, IMPORTED_FILES);
        if (!oldManifest.isFile()) return;
        final File target = paths.isos();
        if (!target.isDirectory()) target.mkdirs();

        final Set<String> names = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(oldManifest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    final File source = new File(legacy, line);
                    final File destination = new File(target, line);
                    if (source.isFile()) {
                        if (!destination.isFile()) {
                            java.nio.file.Files.copy(source.toPath(), destination.toPath());
                        }
                        names.add(line);
                    }
                }
            }
        } catch (IOException ignored) {
            return;
        }
        writeManifest(new File(target, IMPORTED_FILES), names);
    }

    private void writeManifest(final File manifest, final Set<String> names) throws IOException {
        final File temporary = new File(manifest.getParentFile(), manifest.getName() + ".tmp");
        final StringBuilder contents = new StringBuilder();
        for (final String name : names) {
            contents.append(name).append('\n');
        }
        Files.writeString(temporary.toPath(), contents.toString(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        replaceAtomically(temporary, manifest);
    }

    private void copy(final Uri source, final File destination) throws IOException {
        final File temporary = new File(destination.getParentFile(), destination.getName() + ".part");
        Files.deleteIfExists(temporary.toPath());
        long copied = 0;
        try (InputStream input = context.getContentResolver().openInputStream(source);
             java.io.OutputStream output = Files.newOutputStream(temporary.toPath(),
                 StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            if (input == null) throw new IOException("Unable to open selected game-data file");
            final byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    output.write(buffer, 0, count);
                    copied += count;
                }
            }
        }
        if (copied == 0) {
            Files.deleteIfExists(temporary.toPath());
            throw new IOException("Selected game-data file was empty");
        }
        replaceAtomically(temporary, destination);
    }

    private void replaceAtomically(final File temporary, final File destination) throws IOException {
        try {
            Files.move(temporary.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException exception) {
            Files.move(temporary.toPath(), destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<File> listedFiles() {
        final List<File> files = new ArrayList<>();
        final File manifest = new File(paths.isos(), IMPORTED_FILES);
        if (!manifest.isFile()) return files;
        try (BufferedReader reader = new BufferedReader(new FileReader(manifest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    final File file = new File(paths.isos(), line);
                    if (file.isFile()) files.add(file);
                }
            }
        } catch (IOException ignored) { }
        return files;
    }

    private String safeName(final Uri source) {
        String raw = null;
        try (Cursor cursor = context.getContentResolver().query(
                source, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) raw = cursor.getString(0);
        } catch (Exception ignored) { }
        if (raw == null || raw.isBlank()) raw = source.getLastPathSegment();
        final String name = raw == null ? "game-data.bin" : raw.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isEmpty() ? "game-data.bin" : name;
    }

    private String formatBytes(final long bytes) {
        if (bytes < 1024L * 1024L) return (bytes / 1024L) + " KiB";
        return String.format(java.util.Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024.0));
    }
}
