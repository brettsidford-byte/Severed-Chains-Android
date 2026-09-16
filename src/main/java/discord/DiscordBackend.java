package discord;

public interface DiscordBackend {
  void init();
  void tick();
  void updateActivity(RichPresenceActivity activity);
  void destroy();
}
