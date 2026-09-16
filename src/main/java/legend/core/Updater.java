package legend.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Updater {
  private static final Logger LOGGER = LogManager.getFormatterLogger(Updater.class);

  private static final String UPDATE_URL = "https://api.github.com/repos/Legend-of-Dragoon-Modding/Severed-Chains/releases";

  private CompletableFuture<?> activeCheck;
  private volatile HttpURLConnection activeConnection;

  public void delete() {
    final CompletableFuture<?> check = this.activeCheck;
    if(check != null) check.cancel(true);
    final HttpURLConnection connection = this.activeConnection;
    if(connection != null) connection.disconnect();
  }

  public void check(final Consumer<Release> onComplete) {
    synchronized(this) {
      if(this.activeCheck != null) {
        return;
      }

      //noinspection ConstantValue
      if(Version.TIMESTAMP == null) {
        // If the build timestamp is null, we're running in the IDE or using a custom build that was compiled manually
        LOGGER.info("Custom build, skipping update check");
        onComplete.accept(null);
        return;
      }

      LOGGER.info("Checking for updates...");
      this.activeCheck = CompletableFuture.runAsync(() -> this.fetchReleases(onComplete));
    }
  }

  private void onCheckComplete(final String responseBody, final Consumer<Release> onComplete) throws JSONException {
    final Release release = this.parseReleases(new JSONArray(responseBody))
      .stream()
      .filter(r -> r.tag.startsWith(Version.CHANNEL) && r.timestamp.isAfter(Version.TIMESTAMP))
      .sorted()
      .findFirst()
      .orElse(null);

    synchronized(this) {
      if(release != null) {
        LOGGER.info("Found new release %s", release);
        onComplete.accept(release);
      } else {
        LOGGER.info("No updates found");
        onComplete.accept(null);
      }

      this.activeCheck = null;
    }
  }

  private List<Release> parseReleases(final JSONArray releasesJson) throws JSONException {
    final List<Release> releases = new ArrayList<>();

    for(int releaseIndex = 0; releaseIndex < releasesJson.length(); releaseIndex++) {
      final JSONObject releaseJson = releasesJson.getJSONObject(releaseIndex);

      // get asset download URLs from release
      final Map<String, String> assetUrls = new HashMap<>();
      if(releaseJson.has("assets")) {
        final JSONArray assets = releaseJson.getJSONArray("assets");
        for(int assetIndex = 0; assetIndex < assets.length(); assetIndex++) {
          final JSONObject asset = assets.getJSONObject(assetIndex);
          assetUrls.put(asset.getString("name"), asset.getString("browser_download_url"));
        }
      }

      final Release release = new Release(releaseJson.getString("tag_name"), releaseJson.getString("html_url"), ZonedDateTime.parse(releaseJson.getString("updated_at")), releaseJson.getBoolean("prerelease"), assetUrls);
      releases.add(release);
      LOGGER.info("Found release %s", release);
    }

    return releases;
  }

  private void fetchReleases(final Consumer<Release> onComplete) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection)URI.create(UPDATE_URL).toURL().openConnection();
      this.activeConnection = connection;
      connection.setConnectTimeout(10_000);
      connection.setReadTimeout(10_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("Accept", "application/vnd.github+json");
      final int status = connection.getResponseCode();
      if(status / 100 != 2) {
        LOGGER.warn("Request to %s failed: %d", UPDATE_URL, status);
        onComplete.accept(null);
        return;
      }
      this.onCheckComplete(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8), onComplete);
    } catch(final Exception e) {
      LOGGER.warn("Failed to check for updates", e);
      onComplete.accept(null);
    } finally {
      if(connection != null) connection.disconnect();
      this.activeConnection = null;
      synchronized(this) {
        this.activeCheck = null;
      }
    }
  }

  public static class Release implements Comparable<Release> {
    public final String tag;
    public final String uri;
    public final ZonedDateTime timestamp;
    public final boolean prerelease;
    public final Map<String, String> assetUrls;

    private Release(final String tag, final String uri, final ZonedDateTime timestamp, final boolean prerelease, final Map<String, String> assetUrls) {
      this.tag = tag;
      this.uri = uri;
      this.timestamp = timestamp;
      this.prerelease = prerelease;
      this.assetUrls = assetUrls;
    }

    /**
     * returns the download URL for the archive script also determines OS dynamically or null if not found.
     */
    public String getPlatformDownloadUrl() {
      final String os = System.getProperty("os.name", "").toLowerCase(Locale.US);
      final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.US);
      final boolean isArm = arch.startsWith("arm") || arch.startsWith("aarch64");

      final String keyword;
      if(os.contains("win")) {
        keyword = "Windows";
      } else if(os.contains("mac")) {
        keyword = isArm ? "MacOS_M1" : "MacOS_Intel";
      } else {
        keyword = isArm ? "Linux_ARM64" : "Linux";
      }

      for(final var entry : this.assetUrls.entrySet()) {
        if(entry.getKey().contains(keyword) && !entry.getKey().contains("Steam_Deck")) {
          return entry.getValue();
        }
      }

      return null;
    }

    @Override
    public int compareTo(final Updater.Release o) {
      return -this.timestamp.compareTo(o.timestamp);
    }

    @Override
    public String toString() {
      return this.tag + ' ' + this.timestamp + (this.prerelease ? " (prerelease)" : "");
    }
  }
}
