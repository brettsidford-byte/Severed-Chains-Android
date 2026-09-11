package legend.severedchains.android;

/**
 * Android-side rendering contract. Game-facing rendering code will use this
 * boundary instead of depending directly on GLSurfaceView or Android GLES calls.
 */
public interface AndroidRenderApi {
    void create();
    void resize(int width, int height);
    void beginFrame();
    void draw();
    boolean isReady();
}
