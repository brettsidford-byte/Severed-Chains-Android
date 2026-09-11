package legend.severedchains.android;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Android preflight reader matching the upstream Unpacker ISO header check.
 * It validates the disc image format and returns the PlayStation volume ID;
 * it does not extract or transform game data.
 */
public final class AndroidDiscReader {
    private static final int SECTOR_SIZE = 2352;
    private static final int SYNC_PATTERN_SIZE = 12;
    private static final int PVD_SECTOR = 16;
    private static final int PVD_DATA_SIZE = 0x800;

    private AndroidDiscReader() {
    }

    public static String identify(final File file) {
        if (file == null || !file.isFile()
            || file.length() < (PVD_SECTOR + 1L) * SECTOR_SIZE) {
            return null;
        }

        final byte[] sector = new byte[PVD_DATA_SIZE];
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            input.seek(PVD_SECTOR * (long) SECTOR_SIZE + SYNC_PATTERN_SIZE);
            input.readFully(sector);
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
