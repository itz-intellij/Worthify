package dev.simpleye.worthify.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private File pricesFile;
    private YamlConfiguration pricesConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
    }

    public boolean setPrice(Material material, double value) {
        if (pricesConfig == null || pricesFile == null) {
            return false;
        }

        pricesConfig.set("prices." + material.name(), value);
        try {
            pricesConfig.save(pricesFile);
            pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save prices.yml: " + ex.getMessage());
            return false;
        }
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public YamlConfiguration getPricesConfig() {
        return pricesConfig;
    }

    public File getPricesFile() {
        return pricesFile;
    }
}
