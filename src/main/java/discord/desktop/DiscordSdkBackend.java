package discord.desktop;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.GameSDKException;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.activity.Activity;
import discord.DiscordBackend;
import discord.RichPresenceActivity;
import legend.core.Async;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class DiscordSdkBackend implements DiscordBackend {
  private static final Logger LOGGER = LogManager.getFormatterLogger(DiscordSdkBackend.class);
  private final Activity activity = new Activity();
  private Core core;

  public DiscordSdkBackend() {
    this.activity.timestamps().setStart(Instant.now());
    this.activity.assets().setLargeImage("https://legendofdragoon.org/discord-int/disco.png");
  }

  @Override
  public void init() {
    final CreateParams params = new CreateParams();
    params.setClientID(1385814687458918400L);
    params.setFlags(CreateParams.Flags.toLong(CreateParams.Flags.NO_REQUIRE_DISCORD, CreateParams.Flags.SUPPRESS_EXCEPTIONS));
    Async.run(() -> {
      final Core newCore = new Core(params);
      newCore.setLogHook(LogLevel.ERROR, (level, string) -> LOGGER.error(string));
      newCore.setLogHook(LogLevel.WARN, (level, string) -> LOGGER.warn(string));
      newCore.setLogHook(LogLevel.INFO, (level, string) -> LOGGER.info(string));
      newCore.setLogHook(LogLevel.DEBUG, (level, string) -> LOGGER.debug(string));
      newCore.setLogHook(LogLevel.VERBOSE, (level, string) -> LOGGER.trace(string));
      synchronized(this) { this.core = newCore; }
    }).orTimeout(10, TimeUnit.SECONDS).exceptionally(t -> {
      LOGGER.warn("Failed to initialize Discord rich presence", t);
      return null;
    });
  }

  @Override
  public synchronized void tick() {
    if(this.core != null) {
      try { this.core.runCallbacks(); } catch(final GameSDKException ignored) { }
    }
  }

  @Override
  public synchronized void updateActivity(final RichPresenceActivity source) {
    this.activity.setDetails(source.details());
    this.activity.setState(source.state());
    if(this.core != null) this.core.activityManager().updateActivity(this.activity);
  }

  @Override
  public synchronized void destroy() {
    if(this.core != null) {
      this.core.close();
      this.core = null;
    }
  }
}
