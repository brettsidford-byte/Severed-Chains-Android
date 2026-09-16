package legend.severedchains.android;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

import legend.core.renderer.Shader;
import legend.core.renderer.ShaderOptions;
import legend.core.renderer.BufferUsage;
import legend.core.renderer.DepthComparator;
import legend.core.renderer.FrameBuffer;
import legend.core.renderer.FrameBufferAttachment;
import legend.core.renderer.Mesh;
import legend.core.renderer.RenderApi;
import legend.core.renderer.RendererResourceFactory;
import legend.core.renderer.Translucency;
import legend.core.renderer.Texture;
import legend.core.renderer.TextureDataFormat;
import legend.core.renderer.TextureDataType;
import legend.core.renderer.TextureInternalFormat;
import legend.core.renderer.VertexOrder;
import legend.core.gpu.Rect4i;

/**
 * Android implementation of the upstream RenderApi.
 *
 * <p>This class deliberately owns only GLES-thread resources. It does not
 * reference GLSurfaceView or Activity state, keeping the GLES lifecycle isolated
 * from the desktop renderer.</p>
 */
public final class AndroidGlesRenderApi implements RenderApi {
    private final AssetManager assets;
    private int width;
    private int height;
    private int surfaceWidth = 640;
    private int surfaceHeight = 480;
    private boolean created;
    private boolean backfaceCulling;
    private boolean depthTest;
    private int depthComparator = -1;
    private Translucency translucency;
    private boolean batchWidescreen;
    private boolean batchForced4By3;
    private float batchNativeWidth;
    private float batchNativeHeight;
    private float batchExpectedWidth;
    private float batchWidescreenOffset;
    private float batchWidthScale;
    private float batchHeightScale;
    private final Rect4i tempScissor = new Rect4i();
    private final Rect4i activeScissor = new Rect4i();

    public AndroidGlesRenderApi(final AssetManager assets) {
        this.assets = assets;
    }

    public void setSurfaceSize(final int width, final int height) {
        if(width > 0) this.surfaceWidth = width;
        if(height > 0) this.surfaceHeight = height;
    }

    public void create() {
        requireGlThread();
        RendererResourceFactory.setFactory(new RendererResourceFactory.Factory() {
            @Override
            public Texture makeTexture(final Buffer buffer, final String name,
                                       final int width, final int height,
                                       final TextureInternalFormat internalFormat,
                                       final TextureDataFormat dataFormat,
                                       final TextureDataType dataType,
                                       final boolean minFilter, final boolean magFilter,
                                       final boolean wrapS, final boolean wrapT) {
                return AndroidGlesRenderApi.this.makeTexture(buffer, name, width, height,
                    internalFormat, dataFormat, dataType, minFilter, magFilter, wrapS, wrapT);
            }

            @Override
            public FrameBuffer makeFrameBuffer(final String name,
                                               final FrameBufferAttachment[] attachments) {
                return AndroidGlesRenderApi.this.makeFrameBuffer(name, attachments);
            }
        });
        backfaceCulling = GLES30.glIsEnabled(GLES30.GL_CULL_FACE);
        depthTest = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
        created = true;
    }

    /** Upstream RenderApi lifecycle name. */
    @Override
    public void init() {
        create();
    }

    @Override
    public void resize(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Render dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        GLES30.glLineWidth(Math.max(1.0f, height / 480.0f));
        GLES30.glViewport(0, 0, width, height);
    }

    public void beginFrame(final boolean colour, final boolean depth, final boolean stencil) {
        clear(colour, depth, stencil);
    }

    @Override
    public void clear(final boolean colour, final boolean depth, final boolean stencil) {
        requireCreated();
        int mask = 0;
        if (colour) mask |= GLES30.GL_COLOR_BUFFER_BIT;
        if (depth) {
            // glClear honours the depth write mask. A translucent pass can leave
            // it disabled at the end of the preceding batch/frame.
            GLES30.glDepthMask(true);
            GLES30.glClearDepthf(1.0f);
            mask |= GLES30.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) mask |= GLES30.GL_STENCIL_BUFFER_BIT;
        GLES30.glClear(mask);
        if (depth && translucency != null) GLES30.glDepthMask(false);
    }

    @Override
    public void clearColour(final float red, final float green, final float blue) {
        requireCreated();
        GLES30.glClearColor(red, green, blue, 1.0f);
    }

    @Override
    public void viewport(final int x, final int y, final int width, final int height) {
        requireCreated();
        final int[] framebuffer = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, framebuffer, 0);
        if(framebuffer[0] == 0 && x == 0 && y == 0 && width == 640 && height == 480) {
            final float scale = Math.min(this.surfaceWidth / 640.0f, this.surfaceHeight / 480.0f);
            final int viewportWidth = Math.max(1, Math.round(640.0f * scale));
            final int viewportHeight = Math.max(1, Math.round(480.0f * scale));
            GLES30.glViewport((this.surfaceWidth - viewportWidth) / 2,
                (this.surfaceHeight - viewportHeight) / 2, viewportWidth, viewportHeight);
            return;
        }
        GLES30.glViewport(x, y, width, height);
    }

    @Override
    public Mesh makeMesh(final String name, final VertexOrder vertexOrder,
                         final float[] vertexData, final int[] indices) {
        return makeMesh(name, vertexOrder, vertexData, indices, false, false, null,
            BufferUsage.STATIC);
    }

    @Override
    public Mesh makeMesh(final String name, final VertexOrder vertexOrder,
                         final float[] vertexData, final int[] indices,
                         final boolean textured, final boolean translucent,
                         final Translucency translucencyMode, final BufferUsage bufferUsage) {
        requireCreated();
        return AndroidGlesMesh.create(name, vertexOrder, vertexData, indices, textured,
            translucent, translucencyMode, bufferUsage);
    }

    @Override
    public Mesh makeMesh(final String name, final VertexOrder vertexOrder,
                         final float[] vertexData, final int vertexCount) {
        return makeMesh(name, vertexOrder, vertexData, vertexCount, false, false, null,
            BufferUsage.STATIC);
    }

    @Override
    public Mesh makeMesh(final String name, final VertexOrder vertexOrder,
                         final float[] vertexData, final int vertexCount,
                         final boolean textured, final boolean translucent,
                         final Translucency translucencyMode, final BufferUsage bufferUsage) {
        requireCreated();
        return AndroidGlesMesh.create(name, vertexOrder, vertexData, vertexCount, textured,
            translucent, translucencyMode, bufferUsage);
    }

    @Override
    public Texture makeTexture(final Buffer data, final String name,
                               final int width, final int height,
                               final TextureInternalFormat internalFormat,
                               final TextureDataFormat dataFormat,
                               final TextureDataType dataType,
                               final boolean minFilter, final boolean magFilter,
                               final boolean wrapS, final boolean wrapT) {
        requireCreated();
        return AndroidGlesTextureResource.create(data, name, width, height, internalFormat,
            dataFormat, dataType, minFilter, magFilter, wrapS, wrapT);
    }

    @Override
    public FrameBuffer makeFrameBuffer(final String name,
                                       final FrameBufferAttachment[] attachments) {
        requireCreated();
        return AndroidGlesFrameBuffer.create(name, attachments);
    }

    @Override
    public <Options extends ShaderOptions> Shader<Options> makeShader(
        final String name, final Path vertexPath, final Path fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, assetPath(vertexPath),
            assetPath(fragmentPath), optionsFactory);
    }

    @Override
    public <Options extends ShaderOptions> Shader<Options> makeShader(
        final String name, final Path vertexPath, final Path geometryPath,
        final Path fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, assetPath(vertexPath),
            assetPath(geometryPath), assetPath(fragmentPath), optionsFactory);
    }

    public AndroidGlesShaderResource<ShaderOptions> shader(final String vertexPath,
                                                           final String fragmentPath)
        throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, vertexPath, fragmentPath);
    }

    public <Options extends ShaderOptions> AndroidGlesShaderResource<Options> shader(
        final String vertexPath, final String fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, vertexPath, fragmentPath, optionsFactory);
    }

    public <Options extends ShaderOptions> AndroidGlesShaderResource<Options> shader(
        final String vertexPath, final String geometryPath, final String fragmentPath,
        final Function<Shader<Options>, Supplier<Options>> optionsFactory) throws IOException {
        requireCreated();
        return AndroidGlesShaderResource.load(assets, vertexPath, geometryPath, fragmentPath,
            optionsFactory);
    }

    @Override
    public AndroidGlesUniformBuffer makeUniformBuffer(final long size, final int binding) {
        requireCreated();
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Uniform buffer size is outside Android GLES limits");
        }
        return AndroidGlesUniformBuffer.create((int) size, binding);
    }

    @Override
    public void unbindFramebuffer() {
        requireCreated();
        AndroidGlesFrameBuffer.unbind();
    }

    @Override
    public void unbindTexture() {
        requireCreated();
        AndroidGlesTextureResource.unbindAll();
    }

    @Override
    public void initBatch(final boolean widescreen, final boolean forced4By3,
                          final float nativeWidth, final float nativeHeight,
                          final float expectedWidth, final float widescreenOrthoOffsetX) {
        requireCreated();
        batchWidescreen = widescreen;
        batchForced4By3 = forced4By3;
        batchNativeWidth = nativeWidth;
        batchNativeHeight = nativeHeight;
        batchExpectedWidth = expectedWidth;
        batchWidescreenOffset = widescreenOrthoOffsetX;
        batchWidthScale = width / nativeWidth;
        batchHeightScale = height / nativeHeight;
        backfaceCulling(false);
    }

    @Override
    public void backfaceCulling(final boolean enable) {
        requireCreated();
        if (backfaceCulling == enable) return;
        backfaceCulling = enable;
        if (enable) {
            GLES30.glEnable(GLES30.GL_CULL_FACE);
        } else {
            GLES30.glDisable(GLES30.GL_CULL_FACE);
        }
    }

    @Override
    public void enableDepthTest(final DepthComparator comparator) {
        requireCreated();
        if (!depthTest) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            depthTest = true;
        }
        final int value = switch (comparator) {
            case NEVER -> GLES30.GL_NEVER;
            case LESS -> GLES30.GL_LESS;
            case EQUAL -> GLES30.GL_EQUAL;
            case LESS_THAN_OR_EQUAL -> GLES30.GL_LEQUAL;
            case GREATER -> GLES30.GL_GREATER;
            case NOT_EQUAL -> GLES30.GL_NOTEQUAL;
            case GREATER_OR_EQUAL -> GLES30.GL_GEQUAL;
            case ALWAYS -> GLES30.GL_ALWAYS;
        };
        if (depthComparator != value) {
            GLES30.glDepthFunc(value);
            depthComparator = value;
        }
    }

    @Override
    public void disableDepthTest() {
        requireCreated();
        if (depthTest) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            depthTest = false;
        }
    }

    @Override
    public void scissor(final Rect4i worldScissor, final Rect4i modelScissor,
                        final FloatBuffer scissorBuffer,
                        final legend.core.renderer.ShaderUniformBuffer scissorUniform) {
        requireCreated();
        tempScissor.set(worldScissor.x, height - (worldScissor.y + worldScissor.h),
            worldScissor.w, worldScissor.h);
        if (modelScissor.w != 0 || modelScissor.h != 0) {
            if (batchWidescreen) {
                final float widescreenScale = batchHeightScale
                    * (batchExpectedWidth / batchNativeWidth);
                tempScissor.subregion(
                    Math.round((modelScissor.x + batchWidescreenOffset) * widescreenScale),
                    height - Math.round((modelScissor.y + modelScissor.h) * batchHeightScale),
                    Math.round(modelScissor.w * widescreenScale),
                    Math.round(modelScissor.h * batchHeightScale));
            } else {
                final float offset;
                final float widthScale;
                if (batchForced4By3) {
                    final float adjustedWidth = batchNativeHeight * ((float) width / height);
                    offset = (adjustedWidth - batchNativeWidth) / 2.0f;
                    widthScale = batchHeightScale;
                } else {
                    offset = batchWidescreenOffset;
                    widthScale = batchWidthScale;
                }
                tempScissor.subregion(
                    Math.round((modelScissor.x + offset) * widthScale),
                    height - Math.round((modelScissor.y + modelScissor.h) * batchHeightScale),
                    Math.round(modelScissor.w * widthScale),
                    Math.round(modelScissor.h * batchHeightScale));
            }
        }
        if (!activeScissor.equals(tempScissor)) {
            scissorBuffer.put(0, tempScissor.x);
            scissorBuffer.put(1, tempScissor.y);
            scissorBuffer.put(2, tempScissor.w);
            scissorBuffer.put(3, tempScissor.h);
            scissorUniform.set(scissorBuffer);
            activeScissor.set(tempScissor);
        }
    }

    @Override
    public void translucency(final Translucency mode) {
        requireCreated();
        if (translucency == mode) return;
        if (translucency == null) {
            GLES30.glDepthMask(false);
            GLES30.glEnable(GLES30.GL_BLEND);
        } else if (mode == null) {
            GLES30.glDepthMask(true);
            GLES30.glDisable(GLES30.GL_BLEND);
        }

        if (mode != null) {
            switch (mode) {
                case HALF_B_PLUS_HALF_F -> {
                    GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
                    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
                }
                case B_PLUS_F -> {
                    GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
                    GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);
                }
                case B_MINUS_F -> {
                    GLES30.glBlendEquation(GLES30.GL_FUNC_REVERSE_SUBTRACT);
                    GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);
                }
                default -> throw new IllegalArgumentException(mode + " is not a direct GLES blend mode");
            }
        }
        translucency = mode;
    }

    /** Polygon wireframe is not available in OpenGL ES. */
    @Override
    public void wireframe(final boolean enable) {
        requireCreated();
    }

    @Override
    public boolean debugEnabled() {
        return false;
    }

    public void destroy() {
        AndroidGlesTextureResource.unbindAll();
        backfaceCulling = false;
        depthTest = false;
        depthComparator = -1;
        translucency = null;
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

    private static String assetPath(final Path path) {
        return path.toString().replace('\\', '/');
    }
}
