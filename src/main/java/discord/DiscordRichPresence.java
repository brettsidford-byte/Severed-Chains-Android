package discord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DiscordRichPresence {
  private static final Logger LOGGER = LogManager.getFormatterLogger(DiscordRichPresence.class);

  public final RichPresenceActivity activity = new RichPresenceActivity();
  private final DiscordBackend backend;

  public DiscordRichPresence() {
    this.backend = createBackend();
  }

  private static DiscordBackend createBackend() {
    try {
      return (DiscordBackend)Class.forName("discord.desktop.DiscordSdkBackend").getConstructor().newInstance();
    } catch(final ReflectiveOperationException | LinkageError e) {
      LOGGER.info("Discord rich presence is unavailable on this platform");
      return null;
    }
  }

  public void tick() { if(this.backend != null) this.backend.tick(); }
  public void updateActivity() { if(this.backend != null) this.backend.updateActivity(this.activity); }
  public void init() { if(this.backend != null) this.backend.init(); }
  public void destroy() { if(this.backend != null) this.backend.destroy(); }
}
