package dev.simpleye.worthify.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class BalanceStore {

    private final JavaPlugin plugin;
    private final File file;

    private YamlConfiguration config;
    private final Map<UUID, Double> balances = new HashMap<>();

    BalanceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    void load() {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to create balances.yml: " + ex.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        balances.clear();

        if (config.isConfigurationSection("balances")) {
            for (String key : config.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    balances.put(uuid, config.getDouble("balances." + key));
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid keys
                }
            }
        }
    }

    double getBalance(OfflinePlayer player, double defaultBalance) {
        if (player == null) {
            return 0.0D;
        }
        return balances.getOrDefault(player.getUniqueId(), defaultBalance);
    }

    boolean withdraw(OfflinePlayer player, double amount, double defaultBalance) {
        if (player == null) {
            return false;
        }
        if (amount <= 0.0D) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        double current = getBalance(player, defaultBalance);
        if (current < amount) {
            return false;
        }

        double next = current - amount;
        if (next < 0.0D) {
            next = 0.0D;
        }
        balances.put(uuid, next);
        return save();
    }

    boolean setBalance(OfflinePlayer player, double amount) {
        if (player == null) {
            return false;
        }

        double next = Math.max(0.0D, amount);
        balances.put(player.getUniqueId(), next);
        return save();
    }

    boolean deposit(OfflinePlayer player, double amount, double defaultBalance) {
        if (player == null) {
            return false;
        }
        if (amount <= 0.0D) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        double next = getBalance(player, defaultBalance) + amount;
        balances.put(uuid, next);
        return save();
    }

    private boolean save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save balances.yml: " + ex.getMessage());
            return false;
        }
    }
}
