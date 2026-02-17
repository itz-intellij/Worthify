package dev.simpleye.worthify.history;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

interface SellHistoryStorage {
    void reload();

    void append(UUID playerId, SellHistoryEntry entry);

    List<SellHistoryEntry> get(UUID playerId);

    default void close() {
        // no-op
    }
}

public final class SellHistoryStore implements SellHistoryStorage {

    private final JavaPlugin plugin;
    private final File yamlFile;
    private YamlConfiguration yaml;

    private final File sqliteFile;
    private final AtomicBoolean sqliteReady = new AtomicBoolean(false);

    private final int maxEntriesPerPlayer;

    public SellHistoryStore(JavaPlugin plugin, int maxEntriesPerPlayer) {
        this.plugin = plugin;
        this.maxEntriesPerPlayer = Math.max(1, maxEntriesPerPlayer);
        this.yamlFile = new File(plugin.getDataFolder(), "sellhistory.yml");
        this.sqliteFile = new File(plugin.getDataFolder(), "sellhistory.db");
    }

    @Override
    public void reload() {
        if (isSqlite()) {
            initSqlite();
            return;
        }
        reloadYaml();
    }

    private void reloadYaml() {
        if (!yamlFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                yamlFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to create sellhistory.yml: " + ex.getMessage());
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(yamlFile);
    }

    private boolean isSqlite() {
        String mode;
        try {
            mode = plugin.getConfig().getString("sell_history.storage", "auto");
        } catch (Throwable ignored) {
            mode = "auto";
        }
        if (mode == null) {
            return true;
        }
        String m = mode.trim().toLowerCase(java.util.Locale.ROOT);
        if (m.equals("yml") || m.equals("yaml")) {
            return false;
        }
        // auto and sqlite both select sqlite
        return true;
    }

    private void initSqlite() {
        plugin.getDataFolder().mkdirs();
        try (Connection c = openConnection()) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("PRAGMA journal_mode=WAL");
                st.executeUpdate("PRAGMA synchronous=NORMAL");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sell_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "player_uuid TEXT NOT NULL," +
                        "ts INTEGER NOT NULL," +
                        "material TEXT NOT NULL," +
                        "amount INTEGER NOT NULL," +
                        "total REAL NOT NULL" +
                        ")");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sell_history_player_ts ON sell_history(player_uuid, ts)");
            }
            sqliteReady.set(true);
        } catch (Exception ex) {
            sqliteReady.set(false);
            plugin.getLogger().severe("Failed to initialize SQLite sell history: " + ex.getMessage());
        }
    }

    private Connection openConnection() throws Exception {
        String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }

    @Override
    public void append(UUID playerId, SellHistoryEntry entry) {
        if (playerId == null || entry == null) {
            return;
        }

        if (isSqlite()) {
            if (!sqliteReady.get()) {
                initSqlite();
            }
            if (!sqliteReady.get()) {
                return;
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> appendSqlite(playerId, entry));
            return;
        }

        if (yaml == null) {
            reloadYaml();
        }
        if (yaml == null) {
            return;
        }

        String path = "players." + playerId + ".entries";
        List<?> raw = yaml.getList(path);
        List<String> list = new ArrayList<>();

        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof String s) {
                    list.add(s);
                }
            }
        }

        list.add(encode(entry));

        int overflow = list.size() - maxEntriesPerPlayer;
        if (overflow > 0) {
            list = new ArrayList<>(list.subList(overflow, list.size()));
        }

        yaml.set(path, list);
        saveYaml();
    }

    private void appendSqlite(UUID playerId, SellHistoryEntry entry) {
        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sell_history(player_uuid, ts, material, amount, total) VALUES(?,?,?,?,?)")) {
                ps.setString(1, playerId.toString());
                ps.setLong(2, entry.timestampMillis());
                ps.setString(3, entry.materialName() == null ? "UNKNOWN" : entry.materialName());
                ps.setInt(4, entry.soldAmount());
                ps.setDouble(5, entry.total());
                ps.executeUpdate();
            }

            // Trim old entries
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM sell_history WHERE id IN (" +
                            "SELECT id FROM sell_history WHERE player_uuid=? ORDER BY ts DESC, id DESC LIMIT -1 OFFSET ?" +
                            ")")) {
                ps.setString(1, playerId.toString());
                ps.setInt(2, maxEntriesPerPlayer);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to append sell history (SQLite): " + ex.getMessage());
        }
    }

    @Override
    public List<SellHistoryEntry> get(UUID playerId) {
        if (playerId == null) {
            return Collections.emptyList();
        }

        if (isSqlite()) {
            if (!sqliteReady.get()) {
                initSqlite();
            }
            if (!sqliteReady.get()) {
                return Collections.emptyList();
            }
            return getSqlite(playerId);
        }

        if (yaml == null) {
            reloadYaml();
        }
        if (yaml == null) {
            return Collections.emptyList();
        }

        String path = "players." + playerId + ".entries";
        List<?> raw = yaml.getList(path);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        List<SellHistoryEntry> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o instanceof String s) {
                SellHistoryEntry e = decode(s);
                if (e != null) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    @Override
    public void close() {
        // yaml store has nothing to close
    }

    private List<SellHistoryEntry> getSqlite(UUID playerId) {
        List<SellHistoryEntry> out = new ArrayList<>();
        try (Connection c = openConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT ts, material, amount, total FROM sell_history WHERE player_uuid=? ORDER BY ts DESC, id DESC LIMIT ?")) {
                ps.setString(1, playerId.toString());
                ps.setInt(2, maxEntriesPerPlayer);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long ts = rs.getLong(1);
                        String material = rs.getString(2);
                        int amount = rs.getInt(3);
                        double total = rs.getDouble(4);
                        out.add(new SellHistoryEntry(ts, material, amount, total));
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load sell history (SQLite): " + ex.getMessage());
            return Collections.emptyList();
        }

        // convert newest->oldest into oldest->newest (GUI expects this order)
        java.util.Collections.reverse(out);
        return out;
    }

    private void saveYaml() {
        try {
            yaml.save(yamlFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save sellhistory.yml: " + ex.getMessage());
        }
    }

    private static String encode(SellHistoryEntry entry) {
        String material = entry.materialName() == null ? "UNKNOWN" : entry.materialName();
        return entry.timestampMillis() + ";" + material + ";" + entry.soldAmount() + ";" + entry.total();
    }

    private static SellHistoryEntry decode(String s) {
        String[] parts = s.split(";");
        if (parts.length == 3) {
            try {
                long ts = Long.parseLong(parts[0]);
                int amount = Integer.parseInt(parts[1]);
                double total = Double.parseDouble(parts[2]);
                return new SellHistoryEntry(ts, "UNKNOWN", amount, total);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        if (parts.length == 4) {
            try {
                long ts = Long.parseLong(parts[0]);
                String material = parts[1];
                int amount = Integer.parseInt(parts[2]);
                double total = Double.parseDouble(parts[3]);
                return new SellHistoryEntry(ts, material, amount, total);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }
}
