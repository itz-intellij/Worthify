package dev.simpleye.worthify.update;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModrinthUpdateChecker {

    public static final class UpdateInfo {
        private final String currentVersion;
        private final String latestVersion;
        private final String url;
        private final Instant publishedAt;
        private final String downloadUrl;

        public UpdateInfo(String currentVersion, String latestVersion, String url, Instant publishedAt, String downloadUrl) {
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.url = url;
            this.publishedAt = publishedAt;
            this.downloadUrl = downloadUrl;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public String getUrl() {
            return url;
        }

        public Instant getPublishedAt() {
            return publishedAt;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }
    }

    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\\"version_number\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern DATE_PUBLISHED_PATTERN = Pattern.compile("\\\"date_published\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern FILE_URL_PATTERN = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final JavaPlugin plugin;
    private final String projectSlug;
    private final String projectUrl;

    private volatile boolean autoUpdateEnabled;
    private volatile String lastAutoUpdatedVersion;

    private volatile UpdateInfo latestUpdate;
    private volatile String lastError;
    private volatile Instant lastCheckedAt;

    private io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

    public ModrinthUpdateChecker(JavaPlugin plugin, String projectSlug) {
        this.plugin = plugin;
        this.projectSlug = (projectSlug == null ? "" : projectSlug.trim());
        this.projectUrl = "https://modrinth.com/plugin/" + this.projectSlug;
    }

    public void setAutoUpdateEnabled(boolean autoUpdateEnabled) {
        this.autoUpdateEnabled = autoUpdateEnabled;
    }

    public void start(long intervalMinutes) {
        stop();

        if (projectSlug.isEmpty()) {
            plugin.getLogger().warning("Update checker is enabled but no Modrinth project slug is configured.");
            return;
        }

        long sanitizedMinutes = Math.max(1L, intervalMinutes);
        task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                scheduledTask -> checkNow(),
                1L, sanitizedMinutes, TimeUnit.MINUTES);
    }

    public void stop() {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask t = task;
        if (t != null) {
            t.cancel();
            task = null;
        }
    }

    public UpdateInfo getLatestUpdate() {
        return latestUpdate;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public boolean isUpdateAvailable() {
        UpdateInfo info = latestUpdate;
        if (info == null) {
            return false;
        }
        return VersionComparator.isNewer(info.getLatestVersion(), info.getCurrentVersion());
    }

    public void checkNow() {
        try {
            String currentVersion = plugin.getDescription().getVersion();
            String json = fetchVersionsJson();
            String latestVersion = extractFirst(json, VERSION_NUMBER_PATTERN);
            Instant publishedAt = parseInstantOrNull(extractFirst(json, DATE_PUBLISHED_PATTERN));
            String downloadUrl = extractFirstJarUrl(json);

            lastCheckedAt = Instant.now();
            lastError = null;

            if (latestVersion == null || latestVersion.isEmpty()) {
                return;
            }

            latestUpdate = new UpdateInfo(currentVersion, latestVersion, projectUrl, publishedAt, downloadUrl);

            if (VersionComparator.isNewer(latestVersion, currentVersion)) {
                plugin.getLogger().warning("Update available: v" + currentVersion + " -> v" + latestVersion + " (" + projectUrl + ")");

                if (autoUpdateEnabled && downloadUrl != null && !downloadUrl.isEmpty()) {
                    if (lastAutoUpdatedVersion == null || !lastAutoUpdatedVersion.equalsIgnoreCase(latestVersion)) {
                        boolean ok = downloadUpdate(downloadUrl);
                        if (ok) {
                            lastAutoUpdatedVersion = latestVersion;
                            plugin.getLogger().warning("Worthify auto-updater downloaded v" + latestVersion + " into plugins/update/. Restart the server to apply it.");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            lastCheckedAt = Instant.now();
            lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
    }

    private static String extractFirst(String json, Pattern pattern) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Matcher m = pattern.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1);
    }

    private static Instant parseInstantOrNull(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractFirstJarUrl(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        Matcher m = FILE_URL_PATTERN.matcher(json);
        while (m.find()) {
            String url = m.group(1);
            if (url != null && url.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return url;
            }
        }
        return null;
    }

    private boolean downloadUpdate(String downloadUrl) {
        try {
            Path pluginsDir = plugin.getDataFolder().toPath().getParent();
            if (pluginsDir == null) {
                plugin.getLogger().warning("Auto-updater failed: could not resolve plugins folder.");
                return false;
            }

            Path updateDir = pluginsDir.resolve("update");
            Files.createDirectories(updateDir);

            String fileName = plugin.getName() + ".jar";
            Path outFile = updateDir.resolve(fileName);

            HttpURLConnection con = (HttpURLConnection) new URL(downloadUrl).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setRequestProperty("User-Agent", "Worthify/" + plugin.getDescription().getVersion() + " (AutoUpdater)");

            int code = con.getResponseCode();
            if (code != 200) {
                plugin.getLogger().warning("Auto-updater download failed: HTTP " + code);
                return false;
            }

            try (InputStream in = con.getInputStream()) {
                Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Auto-updater download failed: " + ex.getMessage());
            return false;
        }
    }

    private String fetchVersionsJson() throws Exception {
        // The endpoint returns an array; the first entry is typically the newest.
        String url = "https://api.modrinth.com/v2/project/" + projectSlug.toLowerCase(Locale.ROOT) + "/version";

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("User-Agent", "Worthify/" + plugin.getDescription().getVersion() + " (UpdateChecker)");

        int code = con.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
