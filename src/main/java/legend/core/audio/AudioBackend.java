package legend.core.audio;

import java.util.List;

public interface AudioBackend {
  List<String> devices();
  boolean init(String requestedDevice);
  boolean isConnected();
  boolean defaultDeviceChanged();
  AudioSink createSink(int bufferCount);
  void destroy();
}
