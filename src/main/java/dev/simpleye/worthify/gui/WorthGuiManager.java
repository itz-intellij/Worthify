package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorthGuiManager {

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

    private final WorthifyPlugin plugin;

    public WorthGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        open(player, page, null);
    }

    public void open(Player player, int page, String query) {
        if (page < 1) {
            page = 1;
        }

        Map<Material, Double> prices = plugin.getWorthManager().getPricesSnapshot();
        List<Map.Entry<Material, Double>> entries = new ArrayList<>(prices.entrySet());

        Material held = null;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && !heldItem.getType().isAir()) {
            held = heldItem.getType();
        }

        String q = query == null ? null : query.trim();
        if (q != null && !q.isEmpty()) {
            String needle = q.toUpperCase();
            entries.removeIf(e -> !matches(e.getKey().name(), needle));
        }

        entries.sort(Comparator.comparing(e -> e.getKey().name()));

        if (held != null) {
            Material heldFinal = held;
            int idx = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getKey() == heldFinal) {
                    idx = i;
                    break;
                }
            }
            if (idx > 0) {
                entries.add(0, entries.remove(idx));
            }
        }

        int perPage = CONTENT_SLOTS.length;
        int maxPages = Math.max(1, (int) Math.ceil(entries.size() / (double) perPage));
        if (page > maxPages) {
            page = maxPages;
        }

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String titleRaw = cfg.getString("gui.worth.title", "Worth (Page {currentPage}/{maxPages})");
        String title = ColorUtil.colorize(titleRaw
                .replace("{currentPage}", Integer.toString(page))
                .replace("{maxPages}", Integer.toString(maxPages)));

        WorthGuiHolder holder = new WorthGuiHolder();
        Inventory inv = plugin.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler());
        }

        int start = (page - 1) * perPage;
        int end = Math.min(entries.size(), start + perPage);
        for (int idx = start; idx < end; idx++) {
            int slot = CONTENT_SLOTS[idx - start];
            inv.setItem(slot, worthItem(entries.get(idx).getKey(), entries.get(idx).getValue()));
        }

        inv.setItem(BACK_SLOT, navItem(cfg.getConfigurationSection("gui.worth.navigation.back")));
        inv.setItem(NEXT_SLOT, navItem(cfg.getConfigurationSection("gui.worth.navigation.next")));

        player.openInventory(inv);
        WorthGuiSession.set(player, page, maxPages);
    }

    private static boolean matches(String materialName, String needleUpper) {
        if (needleUpper.isEmpty()) {
            return true;
        }
        if (materialName.contains(needleUpper)) {
            return true;
        }
        if (materialName.startsWith(needleUpper)) {
            return true;
        }
        String compact = materialName.replace("_", "");
        String needleCompact = needleUpper.replace("_", "");
        return compact.contains(needleCompact);
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

    private static ItemStack worthItem(Material material, double unitPrice) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&f" + material.name()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Worth: &a$" + String.format(java.util.Locale.US, "%.2f", unitPrice)));
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
