package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
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

        YamlConfiguration cfg = plugin.getConfigManager().getMultiplierGuiConfig();
        String title = cfg == null ? null : cfg.getString("title");
        if (title == null) {
            title = "&aWorth Multipliers";
        }
        int size = cfg == null ? GUI_SIZE : cfg.getInt("size", GUI_SIZE);
        if (size <= 0) {
            size = GUI_SIZE;
        }

        MultiplierGuiHolder holder = new MultiplierGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, size, ColorUtil.colorize(title));
        holder.setInventory(inv);

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler(cfg));
        }

        categoryFromConfig(inv, cfg, "ores", SLOT_ORES, Material.DIAMOND_ORE, "&bOres");
        categoryFromConfig(inv, cfg, "food", SLOT_FOOD, Material.COOKED_BEEF, "&6Food");
        categoryFromConfig(inv, cfg, "seeds", SLOT_SEEDS, Material.WHEAT_SEEDS, "&aSeeds");

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

    private ItemStack categoryItem(String key, Material icon, String title, List<String> loreTemplate, double stepSmall, double stepBig) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(title));

            double value = plugin.getConfiguredCategoryMultiplier(key);

            String valueStr = String.format(java.util.Locale.US, "%.2f", value);
            String stepSmallStr = String.format(java.util.Locale.US, "%.2f", stepSmall);
            String stepBigStr = String.format(java.util.Locale.US, "%.2f", stepBig);

            List<String> lore = new ArrayList<>();
            if (loreTemplate != null && !loreTemplate.isEmpty()) {
                for (String line : loreTemplate) {
                    if (line == null) {
                        continue;
                    }
                    lore.add(ColorUtil.colorize(line
                            .replace("{value}", valueStr)
                            .replace("{step_small}", stepSmallStr)
                            .replace("{step_big}", stepBigStr)));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void categoryFromConfig(Inventory inv, YamlConfiguration cfg, String key, int slotDef, Material iconDef, String titleDef) {
        if (inv == null) {
            return;
        }

        int slot = slotDef;
        Material icon = iconDef;
        String title = titleDef;
        List<String> lore = null;

        double stepSmall = 0.10D;
        double stepBig = 1.00D;
        if (cfg != null) {
            stepSmall = cfg.getDouble("steps.small", stepSmall);
            stepBig = cfg.getDouble("steps.big", stepBig);

            String base = "categories." + key + ".";
            slot = cfg.getInt(base + "slot", slotDef);

            String matName = cfg.getString(base + "material");
            if (matName != null) {
                Material m = Material.matchMaterial(matName);
                if (m != null) {
                    icon = m;
                }
            }

            String t = cfg.getString(base + "title");
            if (t != null) {
                title = t;
            }

            lore = cfg.getStringList(base + "lore");
        }

        if (slot < 0 || slot >= inv.getSize()) {
            return;
        }
        inv.setItem(slot, categoryItem(key, icon, title, lore, stepSmall, stepBig));
    }

    private static ItemStack filler(YamlConfiguration cfg) {
        String matName = cfg == null ? null : cfg.getString("filler.material");
        Material mat = matName == null ? Material.BLACK_STAINED_GLASS_PANE : Material.matchMaterial(matName);
        if (mat == null) {
            mat = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = cfg == null ? null : cfg.getString("filler.title");
            meta.setDisplayName(ColorUtil.colorize(title == null ? " " : title));
            item.setItemMeta(meta);
        }
        return item;
    }
}
