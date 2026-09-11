package legend.severedchains.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class AndroidIsoExtractor {
    private static final int USER_SECTOR_SIZE = 2048;
    private static final int RAW_SECTOR_SIZE = 2352;
    private static final int RAW_USER_OFFSET = 24;
    private static final int MAX_DIRECTORY_DEPTH = 32;

    private AndroidIsoExtractor() { }

    public static void extract(final List<File> images, final File outputDirectory,
                               final Consumer<String> status) throws IOException {
        Files.createDirectories(outputDirectory.toPath());
        int extracted = 0;
        for (final File image : images) {
            final String id = GameDataInspector.identify(image);
            if (id == null) {
                status.accept("Skipping unrecognised image: " + image.getName());
                continue;
            }
            final String disc = id.substring(id.length() - 2);
            final Path destination = outputDirectory.toPath().resolve("disc-" + disc);
            status.accept("Reading " + image.getName() + " (" + id + ")");
            extracted += extractImage(image, destination, status);
        }
        Files.writeString(outputDirectory.toPath().resolve("android-extraction-complete.txt"),
            "Extracted " + extracted + " files from recognised PlayStation disc images.\n",
            StandardCharsets.UTF_8);
        status.accept("ISO extraction complete: " + extracted + " files");
    }

    private static int extractImage(final File image, final Path destination,
                                    final Consumer<String> status) throws IOException {
        try (RandomAccessFile input = new RandomAccessFile(image, "r")) {
            final int sectorSize = input.length() % RAW_SECTOR_SIZE == 0
                ? RAW_SECTOR_SIZE : USER_SECTOR_SIZE;
            final int userOffset = sectorSize == RAW_SECTOR_SIZE ? RAW_USER_OFFSET : 0;
            final byte[] descriptor = readSector(input, 16, sectorSize, userOffset);
            if (descriptor[0] != 1 || !new String(descriptor, 1, 5, StandardCharsets.US_ASCII).equals("CD001")) {
                throw new IOException("ISO9660 volume descriptor not found in " + image.getName());
            }
            final int rootRecord = 156;
            final int rootExtent = littleEndianInt(descriptor, rootRecord + 2);
            final int rootSize = littleEndianInt(descriptor, rootRecord + 10);
            final Set<Integer> visited = new HashSet<>();
            return extractDirectory(input, sectorSize, userOffset, rootExtent, rootSize,
                destination, status, visited, 0);
        }
    }

    private static int extractDirectory(final RandomAccessFile input, final int sectorSize,
                                        final int userOffset, final int extent, final int size,
                                        final Path destination, final Consumer<String> status,
                                        final Set<Integer> visited, final int depth) throws IOException {
        if (depth > MAX_DIRECTORY_DEPTH || !visited.add(extent)) return 0;
        Files.createDirectories(destination);
        final byte[] directory = readBytes(input, extent, size, sectorSize, userOffset);
        int offset = 0;
        int count = 0;
        while (offset + 1 < directory.length) {
            final int recordLength = directory[offset] & 0xff;
            if (recordLength == 0) {
                offset = ((offset / USER_SECTOR_SIZE) + 1) * USER_SECTOR_SIZE;
                continue;
            }
            if (offset + recordLength > directory.length || recordLength < 34) break;
            final int childExtent = littleEndianInt(directory, offset + 2);
            final int childSize = littleEndianInt(directory, offset + 10);
            final int flags = directory[offset + 25] & 0xff;
            final int nameLength = directory[offset + 32] & 0xff;
            if (nameLength > 0 && offset + 33 + nameLength <= directory.length) {
                String name = new String(directory, offset + 33, nameLength, StandardCharsets.US_ASCII);
                name = cleanName(name);
                if (!name.isEmpty() && !name.equals(".") && !name.equals("..")) {
                    final Path child = destination.resolve(name).normalize();
                    if (child.startsWith(destination)) {
                        if ((flags & 2) != 0) {
                            count += extractDirectory(input, sectorSize, userOffset, childExtent,
                                childSize, child, status, visited, depth + 1);
                        } else {
                            extractFile(input, sectorSize, userOffset, childExtent, childSize, child);
                            count++;
                            if (count % 250 == 0) status.accept("Extracted " + count + " files from current disc");
                        }
                    }
                }
            }
            offset += recordLength;
        }
        return count;
    }

    private static void extractFile(final RandomAccessFile input, final int sectorSize,
                                    final int userOffset, final int extent, final int size,
                                    final Path destination) throws IOException {
        final Path parent = destination.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(destination))) {
            int remaining = size;
            int sector = extent;
            while (remaining > 0) {
                final byte[] data = readSector(input, sector++, sectorSize, userOffset);
                final int length = Math.min(remaining, USER_SECTOR_SIZE);
                output.write(data, 0, length);
                remaining -= length;
            }
        }
    }

    private static byte[] readBytes(final RandomAccessFile input, final int extent, final int size,
                                    final int sectorSize, final int userOffset) throws IOException {
        final byte[] output = new byte[size];
        int copied = 0;
        int sector = extent;
        while (copied < size) {
            final byte[] data = readSector(input, sector++, sectorSize, userOffset);
            final int length = Math.min(size - copied, USER_SECTOR_SIZE);
            System.arraycopy(data, 0, output, copied, length);
            copied += length;
        }
        return output;
    }

    private static byte[] readSector(final RandomAccessFile input, final int sector,
                                     final int sectorSize, final int userOffset) throws IOException {
        final byte[] output = new byte[USER_SECTOR_SIZE];
        input.seek((long) sector * sectorSize + userOffset);
        input.readFully(output);
        return output;
    }

    private static int littleEndianInt(final byte[] data, final int offset) {
        return (data[offset] & 0xff)
            | ((data[offset + 1] & 0xff) << 8)
            | ((data[offset + 2] & 0xff) << 16)
            | ((data[offset + 3] & 0xff) << 24);
    }

    private static String cleanName(final String raw) {
        final int version = raw.indexOf(';');
        final String name = version >= 0 ? raw.substring(0, version) : raw;
        return name.replace(':', '_').replace('?', '_').replace('*', '_');
    }
}
