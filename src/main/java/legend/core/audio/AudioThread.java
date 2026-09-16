package legend.core.audio;

import legend.core.DebugHelper;
import legend.core.audio.opus.XaPlayer;
import legend.core.audio.sequencer.Sequencer;
import legend.core.audio.sequencer.assets.BackgroundMusic;
import legend.game.modding.coremod.CoreMod;
import legend.game.unpacker.FileData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static legend.core.GameEngine.CONFIG;
public final class AudioThread implements Runnable {
  private static final Logger LOGGER = LogManager.getFormatterLogger(AudioThread.class);
  private static final Marker AUDIO_THREAD_MARKER = MarkerManager.getMarker("AUDIO_THREAD");

  private final int nanosPerTick;
  private AudioBackend backend;
  private boolean backendReady;
  private final boolean stereo;
  private final int voiceCount;
  private InterpolationPrecision interpolationPrecision;
  private PitchResolution pitchResolution;
  private EffectsOverTimeGranularity effectsGranularity;
  private Sequencer sequencer;
  private XaPlayer xaPlayer;
  private FileData pendingXa;
  private final List<AudioSource> sources = new ArrayList<>();

  private boolean running;
  private boolean paused;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private volatile boolean deviceChanged;

  public static List<String> getDevices() {
    try {
      return AudioBackends.current().devices();
    } catch(final IllegalStateException ignored) {
      return List.of();
    }
  }

  public AudioThread(final boolean stereo, final int voiceCount, final InterpolationPrecision bitDepth, final PitchResolution pitchResolution, final EffectsOverTimeGranularity granularity) {
    this.nanosPerTick = 1_000_000_000 / 120;
    this.stereo = stereo;
    this.voiceCount = voiceCount;
    this.interpolationPrecision = bitDepth;
    this.pitchResolution = pitchResolution;
    this.effectsGranularity = granularity;
  }

  public void init() {
    this.initInternal();
    this.addDefaultSources();

    // Poll for default device change
    this.scheduler.scheduleAtFixedRate(() -> {
      final boolean defaultDeviceChanged = this.backend != null && this.backend.defaultDeviceChanged();

      synchronized(this) {
        if(defaultDeviceChanged || this.paused) {
          this.deviceChanged = true;
          this.notify();
        }
      }
    }, 2, 2, TimeUnit.SECONDS);
  }

  public void reinit() {
    LOGGER.info("Reinitializing audio");

    synchronized(this) {
      final boolean[] active = new boolean[this.sources.size()];
      for(int i = 0; i < this.sources.size(); i++) {
        active[i] = this.sources.get(i).isActive();
      }

      this.destroyInternal();
      this.initInternal();

      for(int i = 0; i < this.sources.size(); i++) {
        final AudioSource source = this.sources.get(i);

        synchronized(source) {
          if(this.backendReady) {
            source.init();
          }

          if(active[i]) {
            source.setActive(true);
          }
        }
      }

      if(this.pendingXa != null && this.xaPlayer.isInitialized()) {
        this.xaPlayer.loadXa(this.pendingXa);
        this.pendingXa = null;
      }
    }
  }

  private void initInternal() {
    this.backend = AudioBackends.create();
    final String requested = CONFIG.getConfig(CoreMod.AUDIO_DEVICE_CONFIG.get());
    this.backendReady = this.backend.init(requested);
    if(this.backendReady) {
      synchronized(this) {
        this.paused = false;
        this.notify();
      }
      return;
    }
    LOGGER.warn("Audio backend initialization failed. Retrying audio initialization.");
    this.destroyInternal();
    this.paused = true;
  }

  public void destroy() {
    this.scheduler.shutdown();

    synchronized(this) {
      this.pendingXa = null;

      if(!this.running && this.backendReady) {
        this.destroyInternal();
        this.xaPlayer.unloadOpusFile();
        return;
      }

      this.running = false;
      this.xaPlayer.unloadOpusFile();
      this.notify();
    }

    while(this.backendReady) {
      DebugHelper.sleep(1);
    }
  }

  private void destroyInternal() {
    for(final AudioSource source : this.sources) {
      synchronized(source) {
        if(source.isInitialized()) {
          source.destroy();
        }
      }
    }

    if(this.backend != null) {
      this.backend.destroy();
      this.backend = null;
    }
    this.backendReady = false;
  }

  private void addDefaultSources() {
    this.sequencer = this.addSource(new Sequencer(this.stereo, this.voiceCount, this.interpolationPrecision, this.pitchResolution, this.effectsGranularity));
    this.xaPlayer = this.addSource(new XaPlayer());
  }

  public <T extends AudioSource> T addSource(final T source) {
    synchronized(this) {
      this.sources.add(source);

      synchronized(source) {
        if(this.backendReady) {
          source.init();
        }
      }

      return source;
    }
  }

  public void removeSource(final AudioSource source) {
    synchronized(this) {
      synchronized(source) {
        if(source.isInitialized()) {
          source.destroy();
        }
      }

      this.sources.remove(source);
    }
  }

  @Override
  public void run() {
    this.running = true;

    while(this.running) {
      final long time = System.nanoTime();

      boolean canBuffer = false;

      synchronized(this) {
        while(this.paused && !this.deviceChanged) {
          try {
            this.wait();
          } catch(final InterruptedException ignored) { }
        }

        if(!this.running) {
          break;
        }

        if(this.deviceChanged) {
          this.deviceChanged = false;

          if(this.paused) {
            LOGGER.info("Retrying audio initialization");
            this.reinit();
          } else {
            final String configuredDevice = CONFIG.getConfig(CoreMod.AUDIO_DEVICE_CONFIG.get());

            if(configuredDevice.isEmpty() || "<default>".equals(configuredDevice)) {
              LOGGER.info("Default audio device changed");
              this.reinit();
            }
          }

          if(this.paused) {
            continue;
          }
        }

        if(this.backend != null && !this.backend.isConnected()) {
            LOGGER.warn("Audio device lost");
            this.reinit();

            if(this.paused) {
              continue;
            }
        }

        for(int i = 0; i < this.sources.size(); i++) {
          final AudioSource source = this.sources.get(i);

          synchronized(source) {
            final boolean sourceCanBuffer = source.canBuffer();
            canBuffer = canBuffer || sourceCanBuffer;

            if(sourceCanBuffer) {
              source.tick();
            }

            source.handleProcessedBuffers();
          }
        }
      }

      if(!this.sequencer.canBuffer()) {
        final long interval = System.nanoTime() - time;
        final int toSleep = (int)(Math.max(0, this.nanosPerTick - interval) / 1_000_000);
        DebugHelper.sleep(toSleep);
      }
    }

    synchronized(this) {
      this.destroyInternal();
      this.pendingXa = null;
      this.xaPlayer.unloadOpusFile();
    }
  }

  public void stop() {
    this.paused = false;
    this.running = false;

    synchronized(this) {
      this.pendingXa = null;
      this.notify();
    }
  }

  public void loadBackgroundMusic(final BackgroundMusic backgroundMusic) {
    synchronized(this) {
      if(this.sequencer.isInitialized()) {
        this.sequencer.loadBackgroundMusic(backgroundMusic);
      }
    }
  }

  public int getSongId() {
    synchronized(this) {
      return this.sequencer.getSongId();
    }
  }

  public void unloadMusic() {
    synchronized(this) {
      this.sequencer.unloadMusic();
    }
  }

  public void setMusicPlayerVolume(final float volume) {
    synchronized(this) {
      this.sequencer.setPlayerVolume(volume);
    }
  }

  public void setXaPlayerVolume(final float volume) {
    synchronized(this) {
      this.xaPlayer.setPlayerVolume(volume);
    }
  }

  public void setMainVolume(final int left, final int right) {
    LOGGER.info(AUDIO_THREAD_MARKER, "Setting main volume to %.2f, %.2f", left / 256.0f, right / 256.0f);

    synchronized(this) {
      this.sequencer.setMainVolume(left, right);
    }
  }

  public int getSequenceVolume() {
    synchronized(this) {
      return this.sequencer.getSequenceVolume();
    }
  }

  public int setSequenceVolume(final int volume) {
    LOGGER.info(AUDIO_THREAD_MARKER, "Setting sequence volume to %.2f", volume / 128.0f);

    synchronized(this) {
      return this.sequencer.setSequenceVolume(volume);
    }
  }

  public int changeSequenceVolumeOverTime(final int volume, final int time) {
    LOGGER.info(AUDIO_THREAD_MARKER, "Setting sequence volume to %.2f over %.2fs", volume / 128.0f, time / 60.0f);

    synchronized(this) {
      return this.sequencer.changeSequenceVolumeOverTime(volume, time);
    }
  }

  public void setReverbVolume(final int left, final int right) {
    synchronized(this) {
      this.sequencer.setReverbVolume(left, right);
    }
  }

  public void fadeIn(final int time, final int volume) {
    LOGGER.info(AUDIO_THREAD_MARKER, "Fading in to %.2f for %.2fs", volume / 256.0f, time / 60.0f);

    synchronized(this) {
      if(this.sequencer.isInitialized()) {
        this.sequencer.fadeIn(time, volume);
      }
    }
  }

  public void fadeOut(final int time) {
    LOGGER.info(AUDIO_THREAD_MARKER, "Fading out for %.2fs", time / 60.0f);

    synchronized(this) {
      if(this.sequencer.isInitialized()) {
        this.sequencer.fadeOut(time);
      }
    }
  }

  public void startSequence() {
    LOGGER.info(AUDIO_THREAD_MARKER, "Starting sequence");

    synchronized(this) {
      if(this.sequencer.isInitialized()) {
        this.sequencer.startSequence();
      }
    }
  }

  public void stopSequence() {
    LOGGER.info(AUDIO_THREAD_MARKER, "Stopping sequence");

    synchronized(this) {
      if(this.sequencer.isInitialized()) {
        this.sequencer.stopSequence();
      }
    }
  }

  public void loadXa(final FileData fileData) {
    synchronized(this) {
      if(this.xaPlayer.isInitialized()) {
        this.xaPlayer.loadXa(fileData);
      } else {
        this.xaPlayer.stop();
        this.xaPlayer.unloadOpusFile();
        this.pendingXa = new FileData(fileData.getBytes());
      }
    }
  }

  public void stopXa() {
    synchronized(this) {
      this.pendingXa = null;
      this.xaPlayer.stop();
      this.xaPlayer.unloadOpusFile();
    }
  }

  public boolean isMusicPlaying() {
    synchronized(this) {
      return this.sequencer.isActive() && this.sequencer.isPlaying();
    }
  }

  public void setReverb(final int config) {
    synchronized(this) {
      this.sequencer.setReverbConfig(config);
    }
  }

  public int getSequenceVolumeOverTimeFlags() {
    synchronized(this) {
      return this.sequencer.getVolumeOverTimeFlags();
    }
  }

  public void changeInterpolationBitDepth(final InterpolationPrecision bitDepth) {
    synchronized(this) {
      if(this.interpolationPrecision != bitDepth) {
        this.interpolationPrecision = bitDepth;
        this.sequencer.changeInterpolationBitDepth(this.interpolationPrecision);
      }
    }
  }

  public void changePitchResolution(final PitchResolution pitchResolution) {
    synchronized(this) {
      if(this.pitchResolution != pitchResolution) {
        this.pitchResolution = pitchResolution;
        this.sequencer.changePitchResolution(this.pitchResolution);
      }
    }
  }

  public void changeEffectsOverTimeGranularity(final EffectsOverTimeGranularity effectsGranularity) {
    synchronized(this) {
      if(this.effectsGranularity != effectsGranularity) {
        this.effectsGranularity = effectsGranularity;
        this.sequencer.changeEffectsOverTimeGranularity(effectsGranularity);
      }
    }
  }
}
