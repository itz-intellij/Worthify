package dev.simpleye.worthify.gui;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TopBalGuiManager {

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

    private static final int MAX_VAULT_SCAN = 5000;
    private static final long VAULT_CACHE_TTL_MILLIS = 60_000L;

    private final WorthifyPlugin plugin;

    private volatile List<Map.Entry<UUID, Double>> vaultCache = Collections.emptyList();
    private volatile long vaultCacheTimeMillis;
    private volatile boolean vaultComputing;

    public TopBalGuiManager(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page, int limit) {
        if (page < 1) {
            page = 1;
        }

        int cap = limit <= 0 ? 10 : Math.min(100, limit);
        List<Map.Entry<UUID, Double>> top;
        boolean isVault = !plugin.getEconomyHook().isUsingInternalEconomy();
        if (isVault) {
            top = getVaultTopBalancesCached(cap);
        } else {
            top = plugin.getEconomyHook().topInternalBalances(Integer.MAX_VALUE);
        }

        if (top == null) {
            top = Collections.emptyList();
        } else if (top.size() > cap) {
            top = new ArrayList<>(top.subList(0, cap));
        }

        int perPage = CONTENT_SLOTS.length;
        int maxPages = Math.max(1, (int) Math.ceil(top.size() / (double) perPage));
        if (page > maxPages) {
            page = maxPages;
        }

        FileConfiguration mainCfg = plugin.getConfigManager().getMainConfig();
        YamlConfiguration guiCfg = plugin.getConfigManager().getTopBalGuiConfig();

        String titleRaw;
        if (guiCfg != null && guiCfg.contains("title")) {
            titleRaw = guiCfg.getString("title", "Top Balances (Page {currentPage}/{maxPages})");
        } else {
            titleRaw = mainCfg.getString("gui.topbal.title", "Top Balances (Page {currentPage}/{maxPages})");
        }

        String title = ColorUtil.colorize(titleRaw
                .replace("{currentPage}", Integer.toString(page))
                .replace("{maxPages}", Integer.toString(maxPages)));

        TopBalGuiHolder holder = new TopBalGuiHolder();
        Inventory inv = Bukkit.getServer().createInventory(holder, GUI_SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler());
        }

        int start = (page - 1) * perPage;
        int end = Math.min(top.size(), start + perPage);
        if (isVault && top.isEmpty() && vaultComputing) {
            for (int slot : CONTENT_SLOTS) {
                inv.setItem(slot, loadingItem());
            }
        } else {
            for (int idx = start; idx < end; idx++) {
                inv.setItem(CONTENT_SLOTS[idx - start], entryItem(idx + 1, top.get(idx)));
            }
        }

        ConfigurationSection backSec = guiCfg != null ? guiCfg.getConfigurationSection("navigation.back") : null;
        if (backSec == null) {
            backSec = mainCfg.getConfigurationSection("gui.topbal.navigation.back");
        }
        ConfigurationSection nextSec = guiCfg != null ? guiCfg.getConfigurationSection("navigation.next") : null;
        if (nextSec == null) {
            nextSec = mainCfg.getConfigurationSection("gui.topbal.navigation.next");
        }

        inv.setItem(BACK_SLOT, navItem(backSec));
        inv.setItem(NEXT_SLOT, navItem(nextSec));

        player.openInventory(inv);
        TopBalGuiSession.set(player, page, maxPages, cap);
    }

    private List<Map.Entry<UUID, Double>> getVaultTopBalancesCached(int limit) {
        long now = System.currentTimeMillis();
        boolean stale = vaultCache.isEmpty() || (now - vaultCacheTimeMillis) > VAULT_CACHE_TTL_MILLIS;
        if (stale && !vaultComputing) {
            vaultComputing = true;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map.Entry<UUID, Double>> computed = computeVaultTopBalances(Integer.MAX_VALUE);
                vaultCache = computed;
                vaultCacheTimeMillis = System.currentTimeMillis();
                vaultComputing = false;
            });
        }

        if (vaultCache.isEmpty()) {
            return Collections.emptyList();
        }
        int cap = Math.max(1, Math.min(100, limit));
        if (vaultCache.size() > cap) {
            return new ArrayList<>(vaultCache.subList(0, cap));
        }
        return vaultCache;
    }

    private List<Map.Entry<UUID, Double>> computeVaultTopBalances(int limit) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        int scanCap = Math.min(players.length, MAX_VAULT_SCAN);
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(scanCap);

        for (int i = 0; i < scanCap; i++) {
            OfflinePlayer p = players[i];
            if (p == null) {
                continue;
            }
            double bal = plugin.getEconomyHook().getBalance(p);
            if (bal <= 0.0D) {
                continue;
            }
            list.add(Map.entry(p.getUniqueId(), bal));
        }

        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (limit <= 0 || limit == Integer.MAX_VALUE) {
            return list;
        }
        int cap = Math.max(1, Math.min(100, limit));
        if (list.size() > cap) {
            return new ArrayList<>(list.subList(0, cap));
        }
        return list;
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

    private static ItemStack loadingItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&7Calculating..."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack entryItem(int rank, Map.Entry<UUID, Double> entry) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
        String name = p.getName() == null ? entry.getKey().toString() : p.getName();

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = skull.getItemMeta();
        if (baseMeta instanceof SkullMeta meta) {
            meta.setOwningPlayer(p);
            meta.setDisplayName(ColorUtil.colorize("#00F986#" + rank + " &f" + name));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Balance: &a$" + SellService.formatMoney(entry.getValue())));
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
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
