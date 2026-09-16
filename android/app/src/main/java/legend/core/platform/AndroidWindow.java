package legend.core.platform;

import java.nio.file.Path;

import legend.core.platform.input.InputClass;
import legend.core.renderer.RenderApi;
import legend.severedchains.android.AndroidGlesRenderApi;
import legend.severedchains.android.SeveredChainsSurfaceView;

/** Upstream Window implementation backed by the activity's single GLSurfaceView. */
public final class AndroidWindow extends Window {
    private final AndroidPlatformManager manager;
    private final SeveredChainsSurfaceView surface;
    private final AndroidGlesRenderApi renderApi;
    private final Action renderAction;
    private final int requestedWidth;
    private final int requestedHeight;
    private boolean closeRequested;

    AndroidWindow(final AndroidPlatformManager manager,
                  final SeveredChainsSurfaceView surface,
                  final String title, final int width, final int height) {
        this.manager = manager;
        this.surface = surface;
        this.requestedWidth = width;
        this.requestedHeight = height;
        this.renderApi = new AndroidGlesRenderApi(surface.getContext().getAssets());
        this.renderApi.setSurfaceSize(surface.getWidth(), surface.getHeight());
        this.renderAction = manager.addAction(new Action(this::draw, 60));
        setTitle(title);
    }

    @Override public RenderApi getRenderApi() { return renderApi; }

    @Override
    protected void destroy() {
        manager.removeAction(renderAction);
        events().onClose();
    }

    @Override protected boolean shouldClose() { return closeRequested; }

    @Override
    public void show() {
        surface.queueEvent(() -> events().onResize(getWidth(), getHeight()));
    }

    @Override public void close() { closeRequested = true; }
    @Override public void updateMonitor() { }
    @Override public void makeFullscreen() { surface.requestImmersiveMode(); }
    @Override public void makeWindowed() { }
    @Override public void centerWindow() { }
    @Override public void setTitle(final String title) { surface.setContentDescription(title); }
    @Override public int getWidth() { return 640; }
    @Override public int getHeight() { return 480; }
    @Override public boolean hasFocus() { return surface.hasWindowFocus(); }
    @Override public InputClass getInputClass() { return InputClass.GAMEPAD; }
    @Override public void startTextInput() { }
    @Override public void stopTextInput() { }
    @Override public void disableCursor() { }
    @Override public void showCursor() { }
    @Override public void hideCursor() { }
    @Override public void useNormalCursor() { }
    @Override public void usePointerCursor() { }
    @Override public void setWindowIcon(final Path path) { }
    @Override public void setFpsLimit(final int limit) { renderAction.setExpectedFps(limit); }
    @Override public int getFpsLimit() { return renderAction.getExpectedFps(); }

    long nanosUntilNextRender() {
        return renderAction.nanosUntilNextRun();
    }

    private void draw() {
        events().onDraw();
        manager.clearPressed();
    }

    void surfaceChanged(final int width, final int height) {
        renderApi.setSurfaceSize(width, height);
        events().onResize(640, 480);
    }
}
