package legend.severedchains.android;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Materializes the open-source runtime data that the desktop engine reads through Paths. */
public final class BundledRuntimeAssets {
    private static final String[] ROOTS = {"gfx", "lang", "patches"};

    private BundledRuntimeAssets() { }

    public static void install(final Context context, final Path destination) throws IOException {
        final AssetManager assets = context.getAssets();
        for (final String root : ROOTS) {
            copyTree(assets, "runtime/" + root, destination.resolve(root));
        }
    }

    private static void copyTree(final AssetManager assets, final String assetPath,
                                 final Path outputPath) throws IOException {
        final String[] children = assets.list(assetPath);
        if (children != null && children.length != 0) {
            Files.createDirectories(outputPath);
            for (final String child : children) {
                copyTree(assets, assetPath + '/' + child, outputPath.resolve(child));
            }
            return;
        }

        Files.createDirectories(outputPath.getParent());
        try (InputStream input = assets.open(assetPath);
             OutputStream output = Files.newOutputStream(outputPath)) {
            input.transferTo(output);
        }
    }
}
