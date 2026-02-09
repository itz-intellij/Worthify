package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.history.SellHistoryEntry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SellHistoryGuiManager {

    public static final int GUI_SIZE = 54;
    public static final int BACK_SLOT = 45;
    public static final int NEXT_SLOT = 53;

    private static final int[] CONTENT_SLOTS = new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final WorthifyPlugin plugin;

    public SellHistoryGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        if (page < 1) {
            page = 1;
        }

        List<SellHistoryEntry> history = plugin.getSellHistoryStore().get(player.getUniqueId());
        if (history.isEmpty()) {
            history = Collections.emptyList();
        }

        int perPage = CONTENT_SLOTS.length;
        int maxPages = Math.max(1, (int) Math.ceil(history.size() / (double) perPage));
        if (page > maxPages) {
            page = maxPages;
        }

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String titleRaw = cfg.getString("gui.sellhistory.title", "Sell History (Page {currentPage}/{maxPages})");
        String title = ColorUtil.colorize(titleRaw
                .replace("{currentPage}", Integer.toString(page))
                .replace("{maxPages}", Integer.toString(maxPages)));

        SellHistoryGuiHolder holder = new SellHistoryGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler());
        }

        int start = (page - 1) * perPage;
        int end = Math.min(history.size(), start + perPage);
        for (int idx = start; idx < end; idx++) {
            inv.setItem(CONTENT_SLOTS[idx - start], historyItem(history.get(history.size() - 1 - idx)));
        }

        inv.setItem(BACK_SLOT, navItem(cfg.getConfigurationSection("gui.sellhistory.navigation.back")));
        inv.setItem(NEXT_SLOT, navItem(cfg.getConfigurationSection("gui.sellhistory.navigation.next")));

        player.openInventory(inv);
        SellHistoryGuiSession.set(player, page, maxPages);
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

    private static ItemStack historyItem(SellHistoryEntry entry) {
        String mat = entry.materialName() == null ? "UNKNOWN" : entry.materialName();
        Material icon;
        String displayItemName;
        if (mat.equalsIgnoreCase("MIXED")) {
            icon = Material.CHEST;
            displayItemName = "Mixed";
        } else {
            icon = Material.matchMaterial(mat);
            if (icon == null) {
                icon = Material.PAPER;
            }
            displayItemName = mat.replace('_', ' ');
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("#00F986Sale &7(" + displayItemName + " x" + entry.soldAmount() + ")"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Time: &f" + TS_FMT.format(Instant.ofEpochMilli(entry.timestampMillis()))));
            lore.add(ColorUtil.colorize("&7Item: &f" + displayItemName));
            lore.add(ColorUtil.colorize("&7Amount: &f" + entry.soldAmount()));
            lore.add(ColorUtil.colorize("&7Total: &a$" + String.format(java.util.Locale.US, "%.2f", entry.total())));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack navItem(ConfigurationSection section) {
        if (section == null) {
            return new ItemStack(Material.ARROW);
        }

        String materialName = section.getString("material", "ARROW");
        Material material = plugin.getMaterialResolver() == null ? Material.matchMaterial(materialName) : plugin.getMaterialResolver().resolve(materialName);
        if (material == null) {
            material = Material.ARROW;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(section.getString("title", "")));
            List<String> loreRaw = section.getStringList("lore");
            List<String> lore = new ArrayList<>(loreRaw.size());
            for (String line : loreRaw) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
