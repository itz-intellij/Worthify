package dev.simpleye.worthify.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private File pricesFile;
    private YamlConfiguration pricesConfig;

    private YamlConfiguration worthGuiConfig;
    private YamlConfiguration sellGuiConfig;
    private YamlConfiguration sellHistoryGuiConfig;
    private YamlConfiguration topBalGuiConfig;

    private YamlConfiguration langConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private void reloadLangConfig() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String code = plugin.getConfig().getString("language", "en");
        if (code == null || code.trim().isEmpty()) {
            code = "en";
        }
        code = code.trim().toLowerCase(java.util.Locale.ROOT);

        String resourcePath = "lang/" + code + ".yml";
        YamlConfiguration loaded = loadOrCreate(resourcePath);
        if (loaded == null || loaded.getKeys(true).isEmpty()) {
            loaded = loadOrCreate("lang/en.yml");
        }
        langConfig = loaded;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();

        reloadGuiConfigs();
        reloadLangConfig();

        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
    }

    private void reloadGuiConfigs() {
        File guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }

        worthGuiConfig = loadOrCreate("gui/worth.yml");
        sellGuiConfig = loadOrCreate("gui/sell.yml");
        sellHistoryGuiConfig = loadOrCreate("gui/sellhistory.yml");
        topBalGuiConfig = loadOrCreate("gui/topbal.yml");
    }

    private YamlConfiguration loadOrCreate(String resourcePath) {
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException ignored) {
                // resource not packaged
            }
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(outFile);

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                cfg.setDefaults(def);
                cfg.options().copyDefaults(true);
                cfg.save(outFile);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load GUI config '" + resourcePath + "': " + ex.getMessage());
        }

        return cfg;
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

    public YamlConfiguration getWorthGuiConfig() {
        return worthGuiConfig;
    }

    public YamlConfiguration getSellGuiConfig() {
        return sellGuiConfig;
    }

    public YamlConfiguration getSellHistoryGuiConfig() {
        return sellHistoryGuiConfig;
    }

    public YamlConfiguration getTopBalGuiConfig() {
        return topBalGuiConfig;
    }

    public YamlConfiguration getLangConfig() {
        return langConfig;
    }

    public File getPricesFile() {
        return pricesFile;
    }
}
