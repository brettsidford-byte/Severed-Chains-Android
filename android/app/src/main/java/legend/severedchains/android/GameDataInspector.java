package legend.severedchains.android;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameDataInspector {
    private static final long SCAN_LIMIT = 32L * 1024L * 1024L;
    private static final Map<String, String> DISC_IDS = new LinkedHashMap<>();

    static {
        DISC_IDS.put("SCUS94491", "Disc 1");
        DISC_IDS.put("SCUS94584", "Disc 2");
        DISC_IDS.put("SCUS94585", "Disc 3");
        DISC_IDS.put("SCUS94586", "Disc 4");
    }

    private GameDataInspector() { }

    public static String inspect(final List<File> files) {
        final StringBuilder result = new StringBuilder("Disc recognition: ");
        int recognised = 0;
        for (final File file : files) {
            final String id = identify(file);
            if (id == null) {
                result.append("\n").append(file.getName()).append(": unrecognised");
            } else {
                recognised++;
                result.append("\n").append(file.getName()).append(": ").append(DISC_IDS.get(id))
                    .append(" (").append(id).append(")");
            }
        }
        result.append("\n").append(recognised).append("/4 expected Severed Chains discs recognised");
        if (recognised == 4) {
            result.append("\nReady for Android extraction integration");
        } else {
            result.append("\nExtraction is not started");
        }
        return result.toString();
    }

    public static String identify(final File file) {
        if (!file.isFile()) return null;
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            final long length = Math.min(input.length(), SCAN_LIMIT);
            final byte[] buffer = new byte[1024 * 1024];
            String carry = "";
            long remaining = length;
            while (remaining > 0) {
                final int count = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (count <= 0) break;
                final String chunk = carry + new String(buffer, 0, count, java.nio.charset.StandardCharsets.US_ASCII)
                    .toUpperCase(java.util.Locale.ROOT)
                    .replaceAll("[^A-Z0-9]", "");
                for (final String id : DISC_IDS.keySet()) {
                    if (chunk.contains(id)) return id;
                }
                carry = chunk.substring(Math.max(0, chunk.length() - 16));
                remaining -= count;
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }
}
