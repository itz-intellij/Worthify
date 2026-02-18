package dev.simpleye.worthify.selltools;

import dev.simpleye.worthify.WorthifyPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SellToolRegistry {

    private final WorthifyPlugin plugin;
    private final File sqliteFile;

    private final Set<UUID> activeIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SellToolRegistry(WorthifyPlugin plugin) {
        this.plugin = plugin;
        this.sqliteFile = new File(plugin.getDataFolder(), "selltools.db");
    }

    public void initAndLoad() {
        plugin.getDataFolder().mkdirs();

        try (Connection c = openConnection()) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("PRAGMA journal_mode=WAL");
                st.executeUpdate("PRAGMA synchronous=NORMAL");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sell_tools (" +
                        "tool_id TEXT PRIMARY KEY," +
                        "type TEXT NOT NULL," +
                        "expires_at INTEGER NOT NULL" +
                        ")");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sell_tools_expires ON sell_tools(expires_at)");
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize SellTools SQLite: " + ex.getMessage());
            return;
        }

        // prune + load ids
        pruneExpired();

        Set<UUID> ids = new HashSet<>();
        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT tool_id FROM sell_tools")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String raw = rs.getString(1);
                        if (raw == null || raw.isBlank()) {
                            continue;
                        }
                        try {
                            ids.add(UUID.fromString(raw));
                        } catch (IllegalArgumentException ignored) {
                            // ignore
                        }
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load SellTools registry: " + ex.getMessage());
            return;
        }

        activeIds.clear();
        activeIds.addAll(ids);
    }

    public void pruneExpired() {
        long now = System.currentTimeMillis();
        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM sell_tools WHERE expires_at<=?")) {
                ps.setLong(1, now);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to prune expired SellTools: " + ex.getMessage());
        }
    }

    public boolean isActive(UUID toolId) {
        return toolId != null && activeIds.contains(toolId);
    }

    public void register(UUID toolId, SellToolType type, long expiresAtMillis) {
        if (toolId == null || type == null || expiresAtMillis <= 0L) {
            return;
        }

        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sell_tools(tool_id, type, expires_at) VALUES(?,?,?) ON CONFLICT(tool_id) DO UPDATE SET type=excluded.type, expires_at=excluded.expires_at")) {
                ps.setString(1, toolId.toString());
                ps.setString(2, type.name());
                ps.setLong(3, expiresAtMillis);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to register SellTool: " + ex.getMessage());
            return;
        }

        activeIds.add(toolId);
    }

    public void revoke(UUID toolId) {
        if (toolId == null) {
            return;
        }

        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM sell_tools WHERE tool_id=?")) {
                ps.setString(1, toolId.toString());
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to revoke SellTool: " + ex.getMessage());
        }

        activeIds.remove(toolId);
    }

    private Connection openConnection() throws Exception {
        String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
