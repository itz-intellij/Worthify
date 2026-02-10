package dev.simpleye.worthify;

import dev.simpleye.worthify.command.WorthifyCommand;
import dev.simpleye.worthify.command.BalanceCommand;
import dev.simpleye.worthify.command.DeleteWorthCommand;
import dev.simpleye.worthify.command.NotImplementedCommand;
import dev.simpleye.worthify.command.PayCommand;
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
import dev.simpleye.worthify.gui.WorthGuiManager;
import dev.simpleye.worthify.hook.WorthLoreProtocolLibHook;
import dev.simpleye.worthify.history.SellHistoryStore;
import dev.simpleye.worthify.listener.SellHistoryGuiListener;
import dev.simpleye.worthify.listener.SellOnCloseGuiListener;
import dev.simpleye.worthify.listener.TopBalGuiListener;
import dev.simpleye.worthify.listener.WorthGuiListener;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.sell.SellService;
import dev.simpleye.worthify.worth.WorthManager;
import org.bukkit.command.PluginCommand;
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
    private ServerVersion serverVersion;
    private MaterialResolver materialResolver;
    private WorthLoreProtocolLibHook worthLoreProtocolLibHook;
    private MessageService messages;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.worthManager = new WorthManager();
        this.economyHook = new EconomyHook(this);
        this.sellHistoryStore = new SellHistoryStore(this, Integer.MAX_VALUE);

        this.serverVersion = ServerVersion.detect();
        this.materialResolver = new MaterialResolver(this.serverVersion);
        getLogger().info("Detected server version: " + this.serverVersion.raw());

        this.configManager.reload();

        this.messages = new MessageService(this);
        this.messages.reload();

        this.worthManager.reload(this.configManager.getPricesConfig(), this.materialResolver, msg -> getLogger().warning(msg));
        this.economyHook.hook();
        this.sellHistoryStore.reload();

        this.sellService = new SellService(this.worthManager, this.economyHook, this.sellHistoryStore);
        this.sellOnCloseGuiManager = new SellOnCloseGuiManager(this);
        this.worthGuiManager = new WorthGuiManager(this);
        this.sellHistoryGuiManager = new SellHistoryGuiManager(this);
        this.topBalGuiManager = new TopBalGuiManager(this);

        registerCommand("sell", new SellCommand(this.sellService, this.sellOnCloseGuiManager, this.messages));
        registerCommand("balance", new BalanceCommand(this));
        registerCommand("topbal", new TopBalanceCommand(this, this.topBalGuiManager));
        registerCommand("deleteworth", new DeleteWorthCommand(this));
        registerCommand("pay", new PayCommand(this));
        registerCommand("setworth", new SetWorthCommand(this));
        registerCommand("worthify", new WorthifyCommand(this));
        registerCommand("worth", new WorthCommand(this, this.worthGuiManager));
        registerCommand("sellhistory", new SellHistoryCommand(this.sellHistoryGuiManager, this.messages));

        NotImplementedCommand notImplemented = new NotImplementedCommand();

        getServer().getPluginManager().registerEvents(new SellOnCloseGuiListener(this, this.sellService), this);
        getServer().getPluginManager().registerEvents(new WorthGuiListener(this, this.worthGuiManager), this);
        getServer().getPluginManager().registerEvents(new SellHistoryGuiListener(this.sellHistoryGuiManager), this);
        getServer().getPluginManager().registerEvents(new TopBalGuiListener(this.topBalGuiManager), this);

        startWorthLoreHookIfEnabled();
    }

    @Override
    public void onDisable() {
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
