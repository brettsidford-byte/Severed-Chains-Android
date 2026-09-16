package legend.core.audio.opus;

import legend.core.audio.AudioFormat;
import legend.core.audio.AudioSource;
import legend.game.modding.coremod.CoreMod;
import legend.game.unpacker.FileData;

import static legend.core.GameEngine.CONFIG;

public final class XaPlayer extends AudioSource {
  private final Object playbackLock = new Object();
  private final int samplesPerTick = 48_000 / 100;
  private OpusDecoder decoder;
  private int channelCount = 1;
  private int format = AudioFormat.MONO_16;
  private short[] pcm = new short[this.samplesPerTick];
  private long sampleCount;
  private long samplesRead;
  private float playerVolume;

  public XaPlayer() {
    super(8);
    this.playerVolume = CONFIG.getConfig(CoreMod.SFX_VOLUME_CONFIG.get()) * CONFIG.getConfig(CoreMod.MASTER_VOLUME_CONFIG.get());
  }

  public void setPlayerVolume(final float volume) { this.playerVolume = volume; }

  public void loadXa(final FileData fileData) {
    synchronized(this.playbackLock) {
      this.unloadOpusFile();
      this.decoder = OpusDecoders.create();
      this.decoder.open(fileData);
      this.samplesRead = 0;
      this.channelCount = this.decoder.channels();
      this.format = this.channelCount == 2 ? AudioFormat.STEREO_16 : AudioFormat.MONO_16;
      this.pcm = new short[this.samplesPerTick * this.channelCount];
      this.sampleCount = this.decoder.sampleCount();
      if(this.sampleCount < this.samplesPerTick * 4L) throw new RuntimeException("XA file is less than 4 buffers in length (40ms)");
      this.setActive(true);
      for(int i = 0; i < 4 && this.canBuffer(); i++) {
        this.readFile();
        this.bufferOutput(this.format, this.pcm, 48_000);
      }
      if(this.isActive()) this.play();
    }
  }

  @Override
  public void tick() {
    synchronized(this.playbackLock) {
      if(this.decoder == null) {
        this.setActive(false);
        return;
      }
      this.readFile();
      this.bufferOutput(this.format, this.pcm, 48_000);
      super.tick();
    }
  }

  private void readFile() {
    final int read = this.decoder.read(this.pcm);
    for(int i = 0; i < read; i++) this.pcm[i] = (short)(this.pcm[i] * this.playerVolume);
    for(int i = read; i < this.pcm.length; i++) this.pcm[i] = 0;
    this.samplesRead += read;
    this.setActive(read > 0 && this.samplesRead <= this.sampleCount);
    if(!this.isActive()) this.unloadOpusFile();
  }

  public void unloadOpusFile() {
    synchronized(this.playbackLock) {
      if(this.decoder != null) {
        this.decoder.close();
        this.decoder = null;
      }
    }
  }

  @Override
  protected void destroy() {
    synchronized(this.playbackLock) {
      this.unloadOpusFile();
      super.destroy();
    }
  }
}
