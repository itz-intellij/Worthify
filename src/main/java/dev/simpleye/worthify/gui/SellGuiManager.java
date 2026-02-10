package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class SellGuiManager {

    public static final int GUI_SIZE = 54;
    public static final int SELL_BUTTON_SLOT = 49;
    public static final int CLOSE_BUTTON_SLOT = 53;

    private final WorthifyPlugin plugin;

    public SellGuiManager(WorthifyPlugin plugin) {
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
        List<Integer> collectSlots;
        if (guiCfg != null && guiCfg.contains("collect_slots")) {
            collectSlots = guiCfg.getIntegerList("collect_slots");
        } else {
            collectSlots = mainCfg.getIntegerList("gui.sell.collect_slots");
        }

        SellGuiHolder holder = new SellGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < GUI_SIZE; i++) {
            if (collectSlots.contains(i)) {
                continue;
            }
            inv.setItem(i, filler());
        }

        inv.setItem(SELL_BUTTON_SLOT, sellButton());
        inv.setItem(CLOSE_BUTTON_SLOT, closeButton());

        player.openInventory(inv);
    }

    private static ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack sellButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("#00F986Sell"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&fClick to sell items placed in the sell slots"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack closeButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&cClose"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
