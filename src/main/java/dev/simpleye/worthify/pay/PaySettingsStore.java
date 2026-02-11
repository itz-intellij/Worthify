package dev.simpleye.worthify.pay;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PaySettingsStore {

    private final JavaPlugin plugin;
    private final File file;

    private YamlConfiguration config;
    private final Map<UUID, Boolean> receiveEnabled = new HashMap<>();

    public PaySettingsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "paysettings.yml");
    }

    public void reload() {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to create paysettings.yml: " + ex.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        receiveEnabled.clear();

        if (config.isConfigurationSection("receive_enabled")) {
            for (String key : config.getConfigurationSection("receive_enabled").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    receiveEnabled.put(uuid, config.getBoolean("receive_enabled." + key, true));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public boolean isReceiveEnabled(UUID playerId) {
        if (playerId == null) {
            return true;
        }
        return receiveEnabled.getOrDefault(playerId, true);
    }

    public boolean setReceiveEnabled(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return false;
        }
        receiveEnabled.put(playerId, enabled);
        return save();
    }

    public boolean toggleReceiveEnabled(UUID playerId) {
        boolean next = !isReceiveEnabled(playerId);
        setReceiveEnabled(playerId, next);
        return next;
    }

    private boolean save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        for (Map.Entry<UUID, Boolean> e : receiveEnabled.entrySet()) {
            config.set("receive_enabled." + e.getKey(), e.getValue());
        }

        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save paysettings.yml: " + ex.getMessage());
            return false;
        }
    }
}
