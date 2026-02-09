package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class SellOnCloseGuiManager {

    public static final int GUI_SIZE = 54;

    private final WorthifyPlugin plugin;

    public SellOnCloseGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String title = ColorUtil.colorize(cfg.getString("gui.sell.title", "Sell"));

        SellGuiHolder holder = new SellGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        player.openInventory(inv);
    }
}
