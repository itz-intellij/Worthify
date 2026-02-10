package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class SellOnCloseGuiManager {

    public static final int GUI_SIZE = 54;

    private final WorthifyPlugin plugin;

    public SellOnCloseGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration mainCfg = plugin.getConfigManager().getMainConfig();
        YamlConfiguration guiCfg = plugin.getConfigManager().getSellGuiConfig();

        String rawTitle;
        if (guiCfg != null && guiCfg.contains("title")) {
            rawTitle = guiCfg.getString("title", "Sell");
        } else {
            rawTitle = mainCfg.getString("gui.sell.title", "Sell");
        }
        String title = ColorUtil.colorize(rawTitle);

        SellGuiHolder holder = new SellGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        player.openInventory(inv);
    }
}
