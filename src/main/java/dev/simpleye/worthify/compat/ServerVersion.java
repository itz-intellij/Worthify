// Server Version Checker To Load The Right Config From 1.21 All To 1.21.4
// Soon Gonna Add Other Higher Versions Items just a matter of time
// with the patches of the resolver ofc
package dev.simpleye.worthify.compat;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerVersion {

    private static final Pattern MC_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    private ServerVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw;
    }

    public static ServerVersion detect() {
        String raw = Bukkit.getMinecraftVersion();
        if (raw == null) {
            return new ServerVersion(0, 0, 0, "unknown");
        }

        Matcher m = MC_VERSION.matcher(raw.trim());
        if (!m.matches()) {
            return new ServerVersion(0, 0, 0, raw);
        }

        int major = parseIntSafe(m.group(1));
        int minor = parseIntSafe(m.group(2));
        int patch = parseIntSafe(m.group(3));
        return new ServerVersion(major, minor, patch, raw);
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        if (this.major != major) {
            return this.major > major;
        }
        if (this.minor != minor) {
            return this.minor > minor;
        }
        return this.patch >= patch;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String raw() {
        return raw;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + " (" + raw + ")";
    }
}
