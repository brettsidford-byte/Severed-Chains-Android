package discord;

public final class RichPresenceActivity {
  private String details;
  private String state;

  public String details() { return this.details; }
  public void setDetails(final String details) { this.details = details; }
  public String state() { return this.state; }
  public void setState(final String state) { this.state = state; }
}
