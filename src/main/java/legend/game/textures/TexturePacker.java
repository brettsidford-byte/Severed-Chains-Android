package legend.game.textures;

import legend.core.gpu.Bpp;
import legend.core.gpu.Rect4i;
import legend.core.DirectBuffers;
import legend.core.renderer.Obj;
import legend.core.renderer.QuadBuilder;
import legend.core.renderer.Texture;
import legend.core.renderer.TextureDataFormat;
import legend.core.renderer.TextureDataType;
import legend.core.renderer.TextureInternalFormat;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TexturePacker {
  public final String name;

  private final Map<RegistryId, Rect4i> entryToRect = new HashMap<>();
  private final Map<RegistryId, Image> entryToImage = new HashMap<>();

  public TexturePacker(final String name) {
    this.name = name;
  }

  public void add(final RegistryId id, final Image image) {
    final Rect4i rect = new Rect4i();
    rect.w = image.width;
    rect.h = image.height;
    this.entryToRect.put(id, rect);
    this.entryToImage.put(id, image);
  }

  public Rect4i getRect(final RegistryId id) {
    return this.entryToRect.get(id);
  }

  public byte[] packToBytes(final int width, final int height) {
    final List<Map.Entry<RegistryId, Rect4i>> entries = new ArrayList<>(this.entryToRect.entrySet());
    entries.sort(Comparator.<Map.Entry<RegistryId, Rect4i>>comparingInt(entry -> entry.getValue().h)
      .thenComparingInt(entry -> entry.getValue().w).reversed()
      .thenComparing(entry -> entry.getKey().toString()));
    int x = 0;
    int y = 0;
    int rowHeight = 0;
    for(final Map.Entry<RegistryId, Rect4i> entry : entries) {
      final Rect4i icon = entry.getValue();
      if(icon.w > width || icon.h > height) throw new RuntimeException("Texture atlas entry is larger than the atlas: " + entry.getKey());
      if(x + icon.w > width) {
        x = 0;
        y += rowHeight;
        rowHeight = 0;
      }
      if(y + icon.h > height) throw new RuntimeException("Failed to pack texture atlas");
      icon.x = x;
      icon.y = y;
      x += icon.w;
      rowHeight = Math.max(rowHeight, icon.h);
    }

    return this.buildTexture(width, height);
  }

  public TextureAtlas pack(final int width, final int height) {
    final byte[] packedData = this.packToBytes(width, height);
    final ByteBuffer buffer = DirectBuffers.bytes(packedData.length);
    buffer.put(0, packedData);

    final Texture texture = Texture.create("Atlas " + this.name, builder -> {
      builder.internalFormat(TextureInternalFormat.RGBA_8);
      builder.dataFormat(TextureDataFormat.RGBA);
      builder.dataType(TextureDataType.UBYTE);
      builder.data(buffer, width, height);
    });

    final Map<RegistryId, TextureAtlasIcon> icons = new HashMap<>();
    final QuadBuilder builder = new QuadBuilder("Atlas " + this.name);

    for(final var entry : this.entryToRect.entrySet()) {
      final Rect4i rect = entry.getValue();

      builder.add();
      builder.bpp(Bpp.BITS_24);
      builder.posSize(1.0f, 1.0f);
      builder.uv(rect.x / (float)width, rect.y / (float)height);
      builder.uvSize(rect.w / (float)width, rect.h / (float)height);
    }

    final Obj obj = builder.build();
    final TextureAtlas atlas = new TextureAtlas(texture, obj, icons);

    int i = 0;
    for(final var entry : this.entryToRect.entrySet()) {
      final RegistryId id = entry.getKey();
      final Rect4i rect = entry.getValue();

      icons.put(id, new TextureAtlasIcon(atlas, rect, i * 4));
      i++;
    }

    return atlas;
  }

  private byte[] buildTexture(final int width, final int height) {
    final byte[] out = new byte[width * height * 4];

    for(final RegistryId entry : this.entryToRect.keySet()) {
      this.insertImage(out, width, entry);
    }

    return out;
  }

  private void insertImage(final byte[] data, final int stride, final RegistryId entry) {
    final Rect4i icon = this.entryToRect.get(entry);
    final Image image = this.entryToImage.get(entry);

    for(int y = 0; y < icon.h; y++) {
      System.arraycopy(image.data, y * icon.w * 4, data, ((icon.y + y) * stride + icon.x) * 4, icon.w * 4);
    }
  }
}
