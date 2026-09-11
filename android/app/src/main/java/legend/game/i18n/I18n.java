package legend.game.i18n;

public final class I18n {
  private I18n() { }
  public static String translate(final String key, final Object... args) {
    if(args.length == 0) return key;
    try { return String.format(key, args); } catch(final RuntimeException ignored) { return key; }
  }
}
