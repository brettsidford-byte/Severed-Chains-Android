package legend.severedchains.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Correctly packed default std140 data for the upstream standard shader. */
public final class AndroidGlesStandardUniforms {
    private final AndroidGlesUniformBuffer[] buffers;

    private AndroidGlesStandardUniforms(final AndroidGlesUniformBuffer[] buffers) {
        this.buffers = buffers;
    }

    public static AndroidGlesStandardUniforms createDefaults() {
        final AndroidGlesUniformBuffer[] buffers = {
            AndroidGlesUniformBuffer.create(128, 0),
            AndroidGlesUniformBuffer.create(10240, 1),
            AndroidGlesUniformBuffer.create(16384, 2),
            AndroidGlesUniformBuffer.create(16384, 3),
            AndroidGlesUniformBuffer.create(16, 4),
            AndroidGlesUniformBuffer.create(16, 5)
        };

        buffers[0].upload(transforms());
        buffers[1].upload(modelTransforms());
        buffers[2].upload(lighting());
        buffers[3].upload(clutAnimation());
        buffers[4].upload(projectionInfo());
        buffers[5].upload(scissor());
        return new AndroidGlesStandardUniforms(buffers);
    }

    public void destroy() {
        for (final AndroidGlesUniformBuffer buffer : buffers) {
            buffer.destroy();
        }
    }

    private static ByteBuffer transforms() {
        final ByteBuffer data = buffer(128);
        identity(data);
        identity(data);
        data.position(0);
        return data;
    }

    private static ByteBuffer modelTransforms() {
        final ByteBuffer data = buffer(10240);
        for (int i = 0; i < 128; i++) {
            identity(data);
            data.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
        }
        data.position(0);
        return data;
    }

    private static ByteBuffer lighting() {
        final ByteBuffer data = buffer(16384);
        identity(data);
        // std140 mat3: three vec4 columns.
        data.putFloat(1).putFloat(0).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(1).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(0).putFloat(1).putFloat(0);
        data.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
        data.position(16384);
        data.position(0);
        return data;
    }

    private static ByteBuffer clutAnimation() {
        final ByteBuffer data = buffer(16384);
        data.putFloat(-1).putFloat(0).putFloat(0).putFloat(0);
        data.position(16384);
        data.position(0);
        return data;
    }

    private static ByteBuffer projectionInfo() {
        final ByteBuffer data = buffer(16);
        data.putFloat(0.1f).putFloat(65536.0f).putFloat(1.0f / 65535.0f).putFloat(0.0f);
        data.position(0);
        return data;
    }

    private static ByteBuffer scissor() {
        final ByteBuffer data = buffer(16);
        data.putFloat(0.0f).putFloat(0.0f).putFloat(640.0f).putFloat(480.0f);
        data.position(0);
        return data;
    }

    private static ByteBuffer buffer(final int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private static void identity(final ByteBuffer data) {
        data.putFloat(1).putFloat(0).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(1).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(0).putFloat(1).putFloat(0);
        data.putFloat(0).putFloat(0).putFloat(0).putFloat(1);
    }
}
