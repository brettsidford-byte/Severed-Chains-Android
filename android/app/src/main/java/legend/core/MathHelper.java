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

    public static void set(final ByteBuffer data, final int offset, final int size, final long value) {
        for (int i = 0; i < size; i++) {
            data.put(offset + i, (byte) (value >>> (i * 8)));
        }
    }

    public static short getShort(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getShort(offset);
    }

    public static void setShort(final ByteBuffer data, final int offset, final short value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value);
    }

    public static int getInt(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getInt(offset);
    }

    public static void setInt(final ByteBuffer data, final int offset, final int value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value);
    }

    public static long getLong(final ByteBuffer data, final int offset) {
        return data.order(ByteOrder.LITTLE_ENDIAN).getLong(offset);
    }

    public static void setLong(final ByteBuffer data, final int offset, final long value) {
        data.order(ByteOrder.LITTLE_ENDIAN).putLong(offset, value);
    }

    public static float psxDegToRad(final short value) {
        return (float) (value * Math.PI / 0x1000);
    }
}
