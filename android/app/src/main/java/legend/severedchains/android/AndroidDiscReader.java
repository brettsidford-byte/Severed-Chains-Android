package legend.severedchains.android;

import legend.game.unpacker.IsoReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Android preflight reader using the upstream IsoReader and the upstream
 * unpacker's PlayStation volume-header rules. It validates the disc image
 * format and returns the volume ID; it does not extract or transform data.
 */
public final class AndroidDiscReader {
    private static final int PVD_SECTOR = 16;
    private static final int PVD_DATA_SIZE = 0x800;

    private AndroidDiscReader() {
    }

    public static String identify(final File file) {
        if (file == null || !file.isFile()
            || file.length() < (PVD_SECTOR + 1L) * IsoReader.SECTOR_SIZE) {
            return null;
        }

        final byte[] sector = new byte[PVD_DATA_SIZE];
        try (IsoReader reader = new IsoReader(file.toPath())) {
            reader.seekSector(PVD_SECTOR);
            reader.advance(IsoReader.SYNC_PATTERN_SIZE);
            reader.read(sector);
        } catch (IOException exception) {
            return null;
        }

        if (sector[0] != 1
            || !ascii(sector, 1, 5).equals("CD001")
            || sector[6] != 1
            || !ascii(sector, 7, 32).trim().equals("PLAYSTATION")) {
            return null;
        }

        return ascii(sector, 39, 32).trim().toUpperCase(Locale.ROOT);
    }

    private static String ascii(final byte[] data, final int offset, final int length) {
        return new String(data, offset, length, StandardCharsets.US_ASCII);
    }
}
