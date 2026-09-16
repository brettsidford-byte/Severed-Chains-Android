package legend.core.renderer;

import javax.annotation.Nullable;
import java.nio.Buffer;
import java.util.Objects;
import java.util.function.Consumer;

/** Platform-neutral construction hook used by the renderer's static resource builders. */
public final class RendererResourceFactory {
  public interface Factory {
    Texture makeTexture(@Nullable Buffer buffer, String name, int width, int height,
                        TextureInternalFormat internalFormat, TextureDataFormat dataFormat,
                        TextureDataType dataType, boolean minFilter, boolean magFilter,
                        boolean wrapS, boolean wrapT);

    FrameBuffer makeFrameBuffer(String name, FrameBufferAttachment[] attachments);
  }

  private static Factory factory;
  private static Consumer<String> notificationSink = ignored -> { };

  private RendererResourceFactory() { }

  public static void setFactory(final Factory factory) {
    RendererResourceFactory.factory = Objects.requireNonNull(factory);
  }

  public static void setNotificationSink(final Consumer<String> notificationSink) {
    RendererResourceFactory.notificationSink = Objects.requireNonNull(notificationSink);
  }

  static Texture makeTexture(@Nullable final Buffer buffer, final String name,
                             final int width, final int height,
                             final TextureInternalFormat internalFormat,
                             final TextureDataFormat dataFormat,
                             final TextureDataType dataType, final boolean minFilter,
                             final boolean magFilter, final boolean wrapS,
                             final boolean wrapT) {
    if(factory == null) throw new IllegalStateException("Renderer resource factory is not installed");
    return factory.makeTexture(buffer, name, width, height, internalFormat, dataFormat,
      dataType, minFilter, magFilter, wrapS, wrapT);
  }

  static FrameBuffer makeFrameBuffer(final String name,
                                     final FrameBufferAttachment[] attachments) {
    if(factory == null) throw new IllegalStateException("Renderer resource factory is not installed");
    return factory.makeFrameBuffer(name, attachments);
  }

  static void notifyUser(final String message) {
    notificationSink.accept(message);
  }
}
