package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import java.io.IOException;
import java.nio.Buffer;

/**
 * Android-side resource facade for the upstream RenderApi port.
 *
 * <p>This class deliberately owns only GLES-thread resources. It does not
 * reference GLSurfaceView or Activity state, which lets the eventual upstream
 * RenderApi bridge share the same lifecycle and keep desktop rendering intact.</p>
 */
public final class AndroidGlesRenderApi {
    private final AssetManager assets;
    private int width;
    private int height;
    private boolean created;

    public AndroidGlesRenderApi(final AssetManager assets) {
        this.assets = assets;
    }

    public void create() {
        requireGlThread();
        created = true;
    }

    public void resize(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Render dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        GLES30.glViewport(0, 0, width, height);
    }

    public void beginFrame(final boolean colour, final boolean depth, final boolean stencil) {
        requireCreated();
        int mask = 0;
        if (colour) mask |= GLES30.GL_COLOR_BUFFER_BIT;
        if (depth) mask |= GLES30.GL_DEPTH_BUFFER_BIT;
        if (stencil) mask |= GLES30.GL_STENCIL_BUFFER_BIT;
        GLES30.glClear(mask);
    }

    public AndroidGlesTextureResource texture(final int width, final int height,
                                              final AndroidGlesTextureResource.Format format,
                                              final Buffer data, final boolean linear,
                                              final boolean repeat) {
        requireCreated();
        return AndroidGlesTextureResource.create(width, height, format, data, linear, repeat);
    }

    public AndroidGlesFrameBuffer framebuffer(final int width, final int height,
                                              final boolean withDepthStencil) {
        requireCreated();
        return AndroidGlesFrameBuffer.create(width, height, withDepthStencil);
    }

    public AndroidGlesShaderResource shader(final String vertexPath,
                                            final String fragmentPath) throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, vertexPath, fragmentPath);
    }

    public void destroy() {
        created = false;
    }

    public boolean isCreated() {
        return created;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private void requireCreated() {
        requireGlThread();
        if (!created) throw new IllegalStateException("Android GLES API is not created");
    }

    private static void requireGlThread() {
        // GLES30 calls are valid only on the current EGL/GL thread. The
        // Android GLSurfaceView owns that thread; this check documents and
        // enforces the lifecycle precondition without relying on thread names.
        if (GLES30.glGetString(GLES30.GL_VERSION) == null) {
            throw new IllegalStateException("No current Android GLES context");
        }
    }
}
