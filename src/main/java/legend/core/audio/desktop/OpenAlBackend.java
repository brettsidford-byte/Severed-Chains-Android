package legend.core.audio.desktop;

import legend.core.audio.AudioBackend;
import legend.core.audio.AudioSink;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SEC_OFFSET;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTDisconnect.ALC_CONNECTED;

public final class OpenAlBackend implements AudioBackend {
  private long device;
  private long context;
  private ALCCapabilities capabilities;
  private IntBuffer connected;
  private String defaultDevice;

  @Override
  public List<String> devices() {
    if(ALC.getCapabilities().ALC_ENUMERATE_ALL_EXT) return ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
    return ALUtil.getStringList(0, ALC_DEVICE_SPECIFIER);
  }

  @Override
  public boolean init(final String requestedDevice) {
    final List<String> devices = this.devices();
    this.defaultDevice = alcGetString(0, ALC_DEFAULT_ALL_DEVICES_SPECIFIER);
    final String selected = devices.contains(requestedDevice) ? requestedDevice
      : this.defaultDevice != null ? this.defaultDevice
      : devices.isEmpty() ? null : devices.get(0);
    this.device = selected == null ? 0 : alcOpenDevice(selected);
    if(this.device == 0) return false;
    this.context = alcCreateContext(this.device, new int[] {0});
    if(this.context == 0) {
      this.destroy();
      return false;
    }
    alcMakeContextCurrent(this.context);
    this.capabilities = ALC.createCapabilities(this.device);
    if(!AL.createCapabilities(this.capabilities).OpenAL10) {
      this.destroy();
      return false;
    }
    this.connected = MemoryUtil.memAllocInt(1);
    return true;
  }

  @Override
  public boolean isConnected() {
    if(this.device == 0) return false;
    if(!this.capabilities.ALC_EXT_disconnect) return true;
    alcGetIntegerv(this.device, ALC_CONNECTED, this.connected);
    return this.connected.get(0) != 0;
  }

  @Override
  public boolean defaultDeviceChanged() {
    alcGetString(0, ALC_ALL_DEVICES_SPECIFIER);
    final String current = alcGetString(0, ALC_DEFAULT_ALL_DEVICES_SPECIFIER);
    return current != null && !current.equals(this.defaultDevice);
  }

  @Override
  public AudioSink createSink(final int bufferCount) {
    return new OpenAlSink(bufferCount);
  }

  @Override
  public void destroy() {
    if(this.context != 0) {
      alcMakeContextCurrent(0);
      alcDestroyContext(this.context);
      this.context = 0;
    }
    if(this.device != 0) {
      alcCloseDevice(this.device);
      this.device = 0;
    }
    if(this.connected != null) {
      MemoryUtil.memFree(this.connected);
      this.connected = null;
    }
  }

  private static final class OpenAlSink implements AudioSink {
    private final int[] buffers;
    private final IntBuffer tmp = MemoryUtil.memAllocInt(1);
    private int bufferIndex;
    private final int source = alGenSources();

    private OpenAlSink(final int bufferCount) {
      this.buffers = new int[bufferCount];
      alGenBuffers(this.buffers);
      this.bufferIndex = bufferCount - 1;
    }

    @Override public boolean canBuffer() { return this.bufferIndex >= 0; }

    @Override
    public void processBuffers() {
      if(this.bufferIndex >= this.buffers.length - 1) return;
      alGetSourcei(this.source, AL_BUFFERS_PROCESSED, this.tmp);
      for(int i = 0; i < this.tmp.get(0); i++) this.buffers[++this.bufferIndex] = alSourceUnqueueBuffers(this.source);
    }

    private int nextBuffer() { return this.buffers[this.bufferIndex--]; }
    @Override public void queue(final int format, final ByteBuffer data, final int rate) { final int id = nextBuffer(); alBufferData(id, format, data, rate); alSourceQueueBuffers(this.source, id); }
    @Override public void queue(final int format, final short[] data, final int rate) { final int id = nextBuffer(); alBufferData(id, format, data, rate); alSourceQueueBuffers(this.source, id); }
    @Override public void queue(final int format, final float[] data, final int rate) { final int id = nextBuffer(); alBufferData(id, format, data, rate); alSourceQueueBuffers(this.source, id); }
    @Override public void play() { alGetSourcei(this.source, AL_SOURCE_STATE, this.tmp); if(this.tmp.get(0) != AL_PLAYING) alSourcePlay(this.source); }
    @Override public void stop() { alSourceStop(this.source); }
    @Override public float position() { return alGetSourcef(this.source, AL_SEC_OFFSET); }

    @Override
    public void destroy() {
      alSourceStop(this.source);
      alDeleteBuffers(this.buffers);
      alDeleteSources(this.source);
      MemoryUtil.memFree(this.tmp);
    }
  }
}
