package legend.core.audio;

/** PCM layouts accepted by the cross-platform audio output layer. */
public final class AudioFormat {
  public static final int MONO_8 = 0x1100;
  public static final int MONO_16 = 0x1101;
  public static final int STEREO_16 = 0x1103;
  public static final int STEREO_FLOAT = 0x10011;

  private AudioFormat() { }
}
