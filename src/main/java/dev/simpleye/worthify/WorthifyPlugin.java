package dev.simpleye.worthify;

import dev.simpleye.worthify.command.WorthifyCommand;
import dev.simpleye.worthify.command.BalanceCommand;
import dev.simpleye.worthify.command.DeleteWorthCommand;
import dev.simpleye.worthify.command.NotImplementedCommand;
import dev.simpleye.worthify.command.PayCommand;
import dev.simpleye.worthify.command.PayToggleCommand;
import dev.simpleye.worthify.command.SellCommand;
import dev.simpleye.worthify.command.SellHistoryCommand;
import dev.simpleye.worthify.command.SetWorthCommand;
import dev.simpleye.worthify.command.TopBalanceCommand;
import dev.simpleye.worthify.command.WorthCommand;
import dev.simpleye.worthify.compat.MaterialResolver;
import dev.simpleye.worthify.compat.ServerVersion;
import dev.simpleye.worthify.config.ConfigManager;
import dev.simpleye.worthify.economy.EconomyHook;
import dev.simpleye.worthify.gui.SellHistoryGuiManager;
import dev.simpleye.worthify.gui.SellOnCloseGuiManager;
import dev.simpleye.worthify.gui.TopBalGuiManager;
import dev.simpleye.worthify.gui.MultiplierGuiManager;
import dev.simpleye.worthify.gui.WorthGuiManager;
import dev.simpleye.worthify.hook.WorthLoreProtocolLibHook;
import dev.simpleye.worthify.history.SellHistoryStore;
import dev.simpleye.worthify.listener.SellHistoryGuiListener;
import dev.simpleye.worthify.listener.SellOnCloseGuiListener;
import dev.simpleye.worthify.listener.TopBalGuiListener;
import dev.simpleye.worthify.listener.WorthGuiListener;
import dev.simpleye.worthify.listener.MultiplierGuiListener;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.placeholder.WorthifyPlaceholders;
import dev.simpleye.worthify.command.MultiplierCommand;
import dev.simpleye.worthify.command.TakeMoneyCommand;
import dev.simpleye.worthify.pay.PaySettingsStore;
import dev.simpleye.worthify.update.ModrinthUpdateChecker;
import dev.simpleye.worthify.update.UpdateNotifyListener;
import dev.simpleye.worthify.sell.SellService;
import dev.simpleye.worthify.worth.WorthManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorthifyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private WorthManager worthManager;
    private EconomyHook economyHook;
    private SellService sellService;
    private SellHistoryStore sellHistoryStore;
    private SellOnCloseGuiManager sellOnCloseGuiManager;
    private WorthGuiManager worthGuiManager;
    private SellHistoryGuiManager sellHistoryGuiManager;
    private TopBalGuiManager topBalGuiManager;
    private MultiplierGuiManager multiplierGuiManager;
    private ServerVersion serverVersion;
    private MaterialResolver materialResolver;
    private WorthLoreProtocolLibHook worthLoreProtocolLibHook;
    private MessageService messages;
    private ModrinthUpdateChecker updateChecker;
    private PaySettingsStore paySettingsStore;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.worthManager = new WorthManager();
        this.economyHook = new EconomyHook(this);
        this.sellHistoryStore = new SellHistoryStore(this, Integer.MAX_VALUE);
        this.paySettingsStore = new PaySettingsStore(this);

        this.serverVersion = ServerVersion.detect();
        this.materialResolver = new MaterialResolver(this.serverVersion);
        getLogger().info("Detected server version: " + this.serverVersion.raw());

        this.configManager.reload();

        this.messages = new MessageService(this);
        this.messages.reload();

        this.worthManager.reload(this.configManager.getPricesConfig(), this.materialResolver, msg -> getLogger().warning(msg));
        this.economyHook.hook();
        this.sellHistoryStore.reload();
        this.paySettingsStore.reload();

        this.sellService = new SellService(this.worthManager, this.economyHook, this.sellHistoryStore, m -> getWorthMultiplier(m));
        this.sellOnCloseGuiManager = new SellOnCloseGuiManager(this);
        this.worthGuiManager = new WorthGuiManager(this);
        this.sellHistoryGuiManager = new SellHistoryGuiManager(this);
        this.topBalGuiManager = new TopBalGuiManager(this);
        this.multiplierGuiManager = new MultiplierGuiManager(this);

        registerCommand("sell", new SellCommand(this.sellService, this.sellOnCloseGuiManager, this.messages));
        registerCommand("balance", new BalanceCommand(this));
        registerCommand("topbal", new TopBalanceCommand(this, this.topBalGuiManager));
        registerCommand("deleteworth", new DeleteWorthCommand(this));
        registerCommand("pay", new PayCommand(this));
        registerCommand("paytoggle", new PayToggleCommand(this));
        registerCommand("setworth", new SetWorthCommand(this));
        registerCommand("worthify", new WorthifyCommand(this));
        registerCommand("worth", new WorthCommand(this, this.worthGuiManager));
        registerCommand("sellhistory", new SellHistoryCommand(this.sellHistoryGuiManager, this.messages));
        registerCommand("multiplier", new MultiplierCommand(this, this.multiplierGuiManager));
        registerCommand("takemoney", new TakeMoneyCommand(this));

        NotImplementedCommand notImplemented = new NotImplementedCommand();

        getServer().getPluginManager().registerEvents(new SellOnCloseGuiListener(this, this.sellService), this);
        getServer().getPluginManager().registerEvents(new WorthGuiListener(this, this.worthGuiManager), this);
        getServer().getPluginManager().registerEvents(new SellHistoryGuiListener(this.sellHistoryGuiManager), this);
        getServer().getPluginManager().registerEvents(new TopBalGuiListener(this.topBalGuiManager), this);
        getServer().getPluginManager().registerEvents(new MultiplierGuiListener(this, this.multiplierGuiManager), this);

        startUpdateCheckerIfEnabled();
        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

        startWorthLoreHookIfEnabled();

        startPlaceholdersIfAvailable();
    }

    private void startPlaceholdersIfAvailable() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        try {
            new WorthifyPlaceholders(this).register();
        } catch (Throwable t) {
            getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
        }
    }

    public PaySettingsStore getPaySettingsStore() {
        return paySettingsStore;
    }

    public double getWorthMultiplier() {
        ConfigManager cfgMgr = this.configManager;
        if (cfgMgr == null) {
            return 1.0D;
        }

        FileConfiguration cfg = cfgMgr.getMainConfig();
        if (cfg == null) {
            return 1.0D;
        }

        boolean enabled = cfg.getBoolean("worth_multiplier.enabled", false);
        if (!enabled) {
            return 1.0D;
        }

        double value = cfg.getDouble("worth_multiplier.value", 1.0D);
        if (!(value > 0.0D) || Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0D;
        }
        return value;
    }

    public boolean isWorthMultiplierEnabled() {
        ConfigManager cfgMgr = this.configManager;
        if (cfgMgr == null) {
            return false;
        }
        FileConfiguration cfg = cfgMgr.getMainConfig();
        return cfg != null && cfg.getBoolean("worth_multiplier.enabled", false);
    }

    public double getWorthMultiplier(Material material) {
        if (!isWorthMultiplierEnabled() || material == null) {
            return 1.0D;
        }

        FileConfiguration cfg = getConfigManager().getMainConfig();
        if (cfg == null) {
            return 1.0D;
        }

        double global = getWorthMultiplier();
        double category = getCategoryWorthMultiplier(cfg, material);
        Double materialOverride = getMaterialWorthMultiplierOverride(cfg, material);
        double materialMult = materialOverride == null ? 1.0D : materialOverride;

        double mult = global * category * materialMult;
        if (!(mult > 0.0D) || Double.isNaN(mult) || Double.isInfinite(mult)) {
            return 1.0D;
        }
        return mult;
    }

    public double applyWorthMultiplier(Material material, double amount) {
        return amount * getWorthMultiplier(material);
    }

    public double applyWorthMultiplier(double amount) {
        return amount * getWorthMultiplier();
    }

    public double getConfiguredCategoryMultiplier(String categoryKey) {
        FileConfiguration cfg = getConfigManager().getMainConfig();
        if (cfg == null || categoryKey == null || categoryKey.isEmpty()) {
            return 1.0D;
        }
        return sanitizeMultiplier(cfg.getDouble("worth_multiplier.categories." + categoryKey, 1.0D));
    }

    public void setConfiguredCategoryMultiplier(String categoryKey, double value) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return;
        }
        double sanitized = sanitizeMultiplier(value);
        FileConfiguration cfg = getConfig();
        cfg.set("worth_multiplier.categories." + categoryKey, sanitized);
        saveConfig();
        reloadConfig();
    }

    public Double getConfiguredMaterialOverride(Material material) {
        if (material == null) {
            return null;
        }
        FileConfiguration cfg = getConfigManager().getMainConfig();
        if (cfg == null) {
            return null;
        }
        return getMaterialWorthMultiplierOverride(cfg, material);
    }

    public void setConfiguredMaterialOverride(Material material, Double valueOrNull) {
        if (material == null) {
            return;
        }
        FileConfiguration cfg = getConfig();
        String path = "worth_multiplier.materials." + material.name();
        if (valueOrNull == null) {
            cfg.set(path, null);
        } else {
            cfg.set(path, sanitizeMultiplier(valueOrNull));
        }
        saveConfig();
        reloadConfig();
    }

    private static double sanitizeMultiplier(double value) {
        if (!(value > 0.0D) || Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0D;
        }
        return value;
    }

    private static Double getMaterialWorthMultiplierOverride(FileConfiguration cfg, Material material) {
        if (cfg == null || material == null) {
            return null;
        }
        String path = "worth_multiplier.materials." + material.name();
        if (!cfg.contains(path)) {
            return null;
        }
        return sanitizeMultiplier(cfg.getDouble(path, 1.0D));
    }

    private static double getCategoryWorthMultiplier(FileConfiguration cfg, Material material) {
        if (cfg == null || material == null) {
            return 1.0D;
        }

        String categoryKey = classifyMaterialCategory(material);
        if (categoryKey == null) {
            return 1.0D;
        }

        return sanitizeMultiplier(cfg.getDouble("worth_multiplier.categories." + categoryKey, 1.0D));
    }

    private static String classifyMaterialCategory(Material material) {
        String name = material.name();

        if (name.endsWith("_SEEDS") || name.equals("NETHER_WART") || name.equals("COCOA_BEANS") || name.equals("MELON_SEEDS") || name.equals("PUMPKIN_SEEDS")) {
            return "seeds";
        }

        if (name.contains("ORE") || name.startsWith("RAW_") || name.endsWith("_INGOT") || name.endsWith("_NUGGET")
                || name.equals("DIAMOND") || name.equals("EMERALD") || name.equals("COAL") || name.equals("REDSTONE")
                || name.equals("LAPIS_LAZULI") || name.equals("QUARTZ") || name.equals("AMETHYST_SHARD")) {
            return "ores";
        }

        if (name.contains("_BEEF") || name.contains("_CHICKEN") || name.contains("_PORKCHOP") || name.contains("_MUTTON") || name.contains("_RABBIT")
                || name.contains("COOKED_") || name.equals("BREAD") || name.equals("APPLE") || name.equals("GOLDEN_APPLE") || name.equals("ENCHANTED_GOLDEN_APPLE")
                || name.equals("CARROT") || name.equals("POTATO") || name.equals("BAKED_POTATO") || name.equals("BEETROOT") || name.equals("BEETROOT_SOUP")
                || name.equals("MUSHROOM_STEW") || name.equals("RABBIT_STEW") || name.equals("SUSPICIOUS_STEW")
                || name.equals("PUMPKIN_PIE") || name.equals("COOKIE") || name.equals("MELON_SLICE") || name.equals("SWEET_BERRIES")
                || name.equals("GLOW_BERRIES") || name.equals("CHORUS_FRUIT") || name.equals("DRIED_KELP") || name.equals("HONEY_BOTTLE")) {
            return "food";
        }

        return null;
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) {
            updateChecker.stop();
            updateChecker = null;
        }
        if (worthLoreProtocolLibHook != null) {
            worthLoreProtocolLibHook.stop();
            worthLoreProtocolLibHook = null;
        }
    }

    public void reloadPlugin() {
        this.configManager.reload();
        if (this.messages != null) {
            this.messages.reload();
        }
        this.worthManager.reload(this.configManager.getPricesConfig(), this.materialResolver, msg -> getLogger().warning(msg));
        this.economyHook.hook();
        this.sellHistoryStore.reload();
        if (this.paySettingsStore != null) {
            this.paySettingsStore.reload();
        }

        if (updateChecker != null) {
            updateChecker.stop();
            updateChecker = null;
        }
        startUpdateCheckerIfEnabled();

        if (worthLoreProtocolLibHook != null) {
            worthLoreProtocolLibHook.stop();
            worthLoreProtocolLibHook = null;
        }
        startWorthLoreHookIfEnabled();
    }

    private void startWorthLoreHookIfEnabled() {
        if (!getConfigManager().getMainConfig().getBoolean("worth_lore.enabled", false)) {
            return;
        }

        if (getConfigManager().getMainConfig().getBoolean("worth_lore.require_protocollib", true)) {
            if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                getLogger().warning("worth_lore.enabled is true but ProtocolLib is not installed. Disabling worth_lore.");
                return;
            }
        }

        try {
            worthLoreProtocolLibHook = new WorthLoreProtocolLibHook(this);
            worthLoreProtocolLibHook.start();
            getLogger().info("Worth lore hook enabled (ProtocolLib). Items will still stack normally.");
        } catch (Throwable t) {
            getLogger().warning("Failed to enable worth lore hook: " + t.getMessage());
            worthLoreProtocolLibHook = null;
        }
    }

    private void startUpdateCheckerIfEnabled() {
        FileConfiguration cfg = getConfigManager().getMainConfig();
        if (cfg == null) {
            return;
        }

        boolean enabled = cfg.getBoolean("update_checker.enabled", true);
        if (!enabled) {
            return;
        }

        String projectSlug = cfg.getString("update_checker.modrinth_project", "worthify");
        long intervalMinutes = cfg.getLong("update_checker.interval_minutes", 360L);
        if (intervalMinutes < 5L) {
            intervalMinutes = 5L;
        }

        boolean autoUpdaterEnabled = cfg.getBoolean("auto_updater.enabled", false);
        if (autoUpdaterEnabled) {
            getLogger().warning("Auto-updater is ENABLED. Worthify may download and replace its jar on next restart. Use at your own risk.");
        }

        this.updateChecker = new ModrinthUpdateChecker(this, projectSlug);
        this.updateChecker.setAutoUpdateEnabled(autoUpdaterEnabled);
        this.updateChecker.start(intervalMinutes);
    }

    public ModrinthUpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public boolean isUpdateCheckerNotifyOnJoin() {
        FileConfiguration cfg = getConfigManager().getMainConfig();
        return cfg != null && cfg.getBoolean("update_checker.notify_on_join", true);
    }

    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    public MaterialResolver getMaterialResolver() {
        return materialResolver;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorthManager getWorthManager() {
        return worthManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public SellHistoryStore getSellHistoryStore() {
        return sellHistoryStore;
    }

    public SellOnCloseGuiManager getSellOnCloseGuiManager() {
        return sellOnCloseGuiManager;
    }

    public WorthGuiManager getWorthGuiManager() {
        return worthGuiManager;
    }

    public SellHistoryGuiManager getSellHistoryGuiManager() {
        return sellHistoryGuiManager;
    }

    public MessageService getMessages() {
        return messages;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
    }
}
