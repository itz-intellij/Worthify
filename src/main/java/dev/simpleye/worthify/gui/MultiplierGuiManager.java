package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MultiplierGuiManager {

    public static final int GUI_SIZE = 27;

    public static final int SLOT_ORES = 11;
    public static final int SLOT_FOOD = 13;
    public static final int SLOT_SEEDS = 15;

    private final WorthifyPlugin plugin;

    public MultiplierGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }

        MultiplierGuiHolder holder = new MultiplierGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, ColorUtil.colorize("&aWorth Multipliers"));
        holder.setInventory(inv);

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler());
        }

        inv.setItem(SLOT_ORES, categoryItem("ores", Material.DIAMOND_ORE, "&bOres"));
        inv.setItem(SLOT_FOOD, categoryItem("food", Material.COOKED_BEEF, "&6Food"));
        inv.setItem(SLOT_SEEDS, categoryItem("seeds", Material.WHEAT_SEEDS, "&aSeeds"));

        player.openInventory(inv);
    }

    public void refresh(Player player) {
        if (player == null) {
            return;
        }
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MultiplierGuiHolder)) {
            return;
        }
        open(player);
    }

    private ItemStack categoryItem(String key, Material icon, String title) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(title));

            double value = plugin.getConfiguredCategoryMultiplier(key);
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Current: &f" + String.format(java.util.Locale.US, "%.2f", value)));
            lore.add(ColorUtil.colorize(""));
            lore.add(ColorUtil.colorize("&7Left click: &a+0.10"));
            lore.add(ColorUtil.colorize("&7Right click: &c-0.10"));
            lore.add(ColorUtil.colorize("&7Shift + click: &a+/-1.00"));
            lore.add(ColorUtil.colorize(""));
            lore.add(ColorUtil.colorize("&8Editing requires &fworthify.admin"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
}
