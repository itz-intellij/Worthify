package dev.simpleye.worthify.history;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class SellHistoryStore {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    private final int maxEntriesPerPlayer;

    public SellHistoryStore(JavaPlugin plugin, int maxEntriesPerPlayer) {
        this.plugin = plugin;
        this.maxEntriesPerPlayer = Math.max(1, maxEntriesPerPlayer);
        this.file = new File(plugin.getDataFolder(), "sellhistory.yml");
    }

    public void reload() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to create sellhistory.yml: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void append(UUID playerId, SellHistoryEntry entry) {
        if (config == null) {
            reload();
        }
        if (config == null) {
            return;
        }

        String path = "players." + playerId + ".entries";
        List<?> raw = config.getList(path);
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

        config.set(path, list);
        save();
    }

    public List<SellHistoryEntry> get(UUID playerId) {
        if (config == null) {
            reload();
        }
        if (config == null) {
            return Collections.emptyList();
        }

        String path = "players." + playerId + ".entries";
        List<?> raw = config.getList(path);
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

    private void save() {
        try {
            config.save(file);
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
