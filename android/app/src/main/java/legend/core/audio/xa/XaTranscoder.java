package legend.core.audio.xa;

import legend.game.unpacker.PathNode;
import legend.game.unpacker.Transformations;

public final class XaTranscoder {
  private XaTranscoder() { }
  public static void transform(final PathNode node, final Transformations transformations) {
    // Audio decoding is isolated here; the Android audio backend will replace this boundary.
  }
}
