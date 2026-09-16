package legend.severedchains.android;

import android.opengl.GLES30;

import legend.core.renderer.FrameBuffer;
import legend.core.renderer.FrameBufferAttachment;

/** Android GLES implementation of the upstream framebuffer attachment contract. */
public final class AndroidGlesFrameBuffer implements FrameBuffer {
    private final String name;
    private final int framebuffer;
    private boolean deleted;

    private AndroidGlesFrameBuffer(final String name, final int framebuffer) {
        this.name = name;
        this.framebuffer = framebuffer;
    }

    public static AndroidGlesFrameBuffer create(final String name,
                                                final FrameBufferAttachment[] attachments) {
        if (attachments == null || attachments.length == 0) {
            throw new IllegalArgumentException("Framebuffer requires at least one attachment");
        }
        final int[] handles = new int[1];
        final int[] previous = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, previous, 0);
        GLES30.glGenFramebuffers(1, handles, 0);
        final int framebuffer = handles[0];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);

        int width = 0;
        int height = 0;
        for (final FrameBufferAttachment attachment : attachments) {
            if (!(attachment.texture instanceof AndroidGlesTextureResource texture)) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, previous[0]);
                GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
                throw new IllegalArgumentException("Android framebuffer received a non-Android texture");
            }
            if (width == 0) {
                width = texture.width;
                height = texture.height;
            } else if (texture.width != width || texture.height != height) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, previous[0]);
                GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
                throw new IllegalArgumentException("Framebuffer attachments have different dimensions");
            }
            final int attachmentPoint = switch (attachment.type) {
                case COLOUR -> GLES30.GL_COLOR_ATTACHMENT0;
                case DEPTH -> GLES30.GL_DEPTH_ATTACHMENT;
            };
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, attachmentPoint,
                GLES30.GL_TEXTURE_2D, texture.id(), 0);
        }

        final int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, previous[0]);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
            throw new IllegalStateException("Android framebuffer " + name
                + " is incomplete: 0x" + Integer.toHexString(status));
        }
        return new AndroidGlesFrameBuffer(name, framebuffer);
    }

    @Override
    public void bind() {
        if (deleted) throw new IllegalStateException("Cannot bind framebuffer " + name);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);
    }

    public static void unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void delete() {
        if (deleted) return;
        if (framebuffer != 0) GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
        deleted = true;
    }
}
