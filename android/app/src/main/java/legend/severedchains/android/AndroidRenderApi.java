package legend.severedchains.android;

/**
 * Android-side rendering contract. Game-facing rendering code will use this
 * boundary instead of depending directly on GLSurfaceView or Android GLES calls.
 */
public interface AndroidRenderApi {
    void create();
    void resize(int width, int height);

    default void resize(final int x, final int y, final int width, final int height) {
        resize(width, height);
        android.opengl.GLES30.glViewport(x, y, width, height);
    }
    void beginFrame();
    void draw();
    boolean isReady();

    /** Releases resources owned by the current EGL context. */
    default void destroy() {
    }
}
