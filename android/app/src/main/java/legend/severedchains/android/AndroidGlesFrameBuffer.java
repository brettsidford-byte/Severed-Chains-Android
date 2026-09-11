package legend.severedchains.android;

import android.opengl.GLES30;
import android.util.Log;

/** Android GLES off-screen render target with colour and optional depth/stencil attachments. */
public final class AndroidGlesFrameBuffer {
    private static final String TAG = "SeveredChains";

    private final int width;
    private final int height;
    private final int framebuffer;
    private final int colourTexture;
    private final int depthStencilBuffer;
    private final boolean complete;

    private AndroidGlesFrameBuffer(final int width, final int height, final int framebuffer,
                                   final int colourTexture, final int depthStencilBuffer,
                                   final boolean complete) {
        this.width = width;
        this.height = height;
        this.framebuffer = framebuffer;
        this.colourTexture = colourTexture;
        this.depthStencilBuffer = depthStencilBuffer;
        this.complete = complete;
    }

    public static AndroidGlesFrameBuffer create(final int width, final int height,
                                                final boolean withDepthStencil) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Framebuffer dimensions must be positive");
        }

        final int colour = AndroidGlesResources.createRgbaTexture(null, width, height, false);
        if (colour == 0) {
            return new AndroidGlesFrameBuffer(width, height, 0, 0, 0, false);
        }

        final int[] handles = new int[1];
        GLES30.glGenFramebuffers(1, handles, 0);
        final int framebuffer = handles[0];
        int depthStencil = 0;

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, colour, 0);

        if (withDepthStencil) {
            GLES30.glGenRenderbuffers(1, handles, 0);
            depthStencil = handles[0];
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthStencil);
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH24_STENCIL8,
                width, height);
            GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_STENCIL_ATTACHMENT,
                GLES30.GL_RENDERBUFFER, depthStencil);
        }

        final boolean complete = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            == GLES30.GL_FRAMEBUFFER_COMPLETE;
        if (!complete) {
            Log.e(TAG, "Android GLES framebuffer incomplete: 0x"
                + Integer.toHexString(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)));
        }
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        if (!complete) {
            GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
            if (depthStencil != 0) {
                GLES30.glDeleteRenderbuffers(1, new int[]{depthStencil}, 0);
            }
            AndroidGlesResources.deleteTexture(colour);
        }
        return new AndroidGlesFrameBuffer(width, height, complete ? framebuffer : 0,
            complete ? colour : 0, complete ? depthStencil : 0, complete);
    }

    public void bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);
        GLES30.glViewport(0, 0, width, height);
    }

    public static void unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    public void destroy() {
        if (framebuffer != 0) {
            GLES30.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
        }
        if (depthStencilBuffer != 0) {
            GLES30.glDeleteRenderbuffers(1, new int[]{depthStencilBuffer}, 0);
        }
        AndroidGlesResources.deleteTexture(colourTexture);
    }

    public boolean isComplete() {
        return complete;
    }

    public int colourTexture() {
        return colourTexture;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
