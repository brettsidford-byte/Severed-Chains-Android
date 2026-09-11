package legend.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class MathHelper {
    private MathHelper() {
    }

    public static long get(final ByteBuffer data, final int offset, final int size) {
        long value = 0L;
        for (int i = 0; i < size; i++) {
            value |= (long) (data.get(offset + i) & 0xff) << (i * 8);
        }
        return value;
    }

    public static long get(final byte[] data, final int offset, final int size) {
        long value = 0L;
        for (int i = 0; i < size; i++) {
            value |= (long) (data[offset + i] & 0xff) << (i * 8);
        }
        return value;
    }

    public static void set(final ByteBuffer data, final int offset, final int size, final long value) {
        for (int i = 0; i < size; i++) {
            data.put(offset + i, (byte) (value >>> (i * 8)));
        }
    }

    public static void set(final byte[] data, final int offset, final int size, final long value) {
        for (int i = 0; i < size; i++) {
            data[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    public static short getShort(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getShort(offset);
    }

    public static short getShort(final byte[] data, final int offset) {
        return (short) ((data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8));
    }

    public static void setShort(final ByteBuffer data, final int offset, final short value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value);
    }

    public static void setShort(final byte[] data, final int offset, final short value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    public static int getInt(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getInt(offset);
    }

    public static int getInt(final byte[] data, final int offset) {
        return (data[offset] & 0xff)
            | ((data[offset + 1] & 0xff) << 8)
            | ((data[offset + 2] & 0xff) << 16)
            | ((data[offset + 3] & 0xff) << 24);
    }

    public static void setInt(final ByteBuffer data, final int offset, final int value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value);
    }

    public static void setInt(final byte[] data, final int offset, final int value) {
        for (int i = 0; i < 4; i++) {
            data[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    public static long getLong(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getLong(offset);
    }

    public static long getLong(final byte[] data, final int offset) {
        return (getInt(data, offset) & 0xffff_ffffL) | ((long) getInt(data, offset + 4) << 32);
    }

    public static void setLong(final ByteBuffer data, final int offset, final long value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putLong(offset, value);
    }

    public static void setLong(final byte[] data, final int offset, final long value) {
        for (int i = 0; i < 8; i++) {
            data[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    public static float psxDegToRad(final short value) {
        return (float) (value * Math.PI / 0x1000);
    }
}
