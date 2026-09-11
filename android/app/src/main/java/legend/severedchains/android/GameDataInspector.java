package legend.severedchains.android;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameDataInspector {
    private static final Map<String, String> DISC_IDS = new LinkedHashMap<>();

    static {
        DISC_IDS.put("SCUS94491", "Disc 1");
        DISC_IDS.put("SCUS94584", "Disc 2");
        DISC_IDS.put("SCUS94585", "Disc 3");
        DISC_IDS.put("SCUS94586", "Disc 4");
    }

    private GameDataInspector() {
    }

    public static String inspect(final List<File> files) {
        final StringBuilder result = new StringBuilder("Disc recognition: ");
        int recognised = 0;
        for (final File file : files) {
            final String id = identify(file);
            if (id == null) {
                result.append("\n").append(file.getName()).append(": unrecognised");
            } else {
                recognised++;
                result.append("\n").append(file.getName()).append(": ")
                    .append(DISC_IDS.getOrDefault(id, "recognised"))
                    .append(" (").append(id).append(")");
            }
        }
        result.append("\n").append(recognised).append("/4 expected Severed Chains discs recognised");
        if (recognised == 4) {
            result.append("\nReady for the existing Severed Chains unpacker");
        } else {
            result.append("\nExtraction is not started");
        }
        return result.toString();
    }

    public static String identify(final File file) {
        final String id = AndroidDiscReader.identify(file);
        return DISC_IDS.containsKey(id) ? id : null;
    }
}
