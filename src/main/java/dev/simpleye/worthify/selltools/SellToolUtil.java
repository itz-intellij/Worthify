package dev.simpleye.worthify.selltools;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.ColorUtil;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SellToolUtil {

    private static final String PATH_ROOT = "sell_tools";

    private final WorthifyPlugin plugin;

    private final SellToolRegistry registry;

    private final NamespacedKey keyType;
    private final NamespacedKey keyExpiresAt;
    private final NamespacedKey keyToolId;
    private final NamespacedKey keyUses;
    private final NamespacedKey keyHideExpires;
    private final NamespacedKey keyLastUsedBy;
    private final NamespacedKey keySoldItems;
    private final NamespacedKey keyMoneyMade;

    private static final DateTimeFormatter EXPIRES_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public SellToolUtil(WorthifyPlugin plugin, SellToolRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.keyType = new NamespacedKey(plugin, "sell_tool_type");
        this.keyExpiresAt = new NamespacedKey(plugin, "sell_tool_expires_at");
        this.keyToolId = new NamespacedKey(plugin, "sell_tool_id");
        this.keyUses = new NamespacedKey(plugin, "sell_tool_uses");
        this.keyHideExpires = new NamespacedKey(plugin, "sell_tool_hide_expires");
        this.keyLastUsedBy = new NamespacedKey(plugin, "sell_tool_last_used_by");
        this.keySoldItems = new NamespacedKey(plugin, "sell_tool_sold_items");
        this.keyMoneyMade = new NamespacedKey(plugin, "sell_tool_money_made");
    }

    public ItemStack createTool(SellToolType type, long expiresAtMillis) {
        return createTool(type, expiresAtMillis, -1);
    }

    public ItemStack createTool(SellToolType type, long expiresAtMillis, int uses) {
        return createTool(type, expiresAtMillis, uses, false);
    }

    public ItemStack createUsageTool(SellToolType type, int uses) {
        return createTool(type, Long.MAX_VALUE, uses, true);
    }

    private ItemStack createTool(SellToolType type, long expiresAtMillis, int uses, boolean hideExpires) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";

        String matName = cfg == null ? null : cfg.getString(PATH_ROOT + "." + typeKey + ".material");
        Material mat = matName == null ? null : Material.matchMaterial(matName);
        if (mat == null) {
            mat = type == SellToolType.WAND ? Material.STICK : Material.DIAMOND_AXE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            java.util.UUID toolId = java.util.UUID.randomUUID();

            String title = cfg == null ? null : cfg.getString(PATH_ROOT + "." + typeKey + ".title");
            if (title != null) {
                meta.setDisplayName(ColorUtil.colorize(title));
            }

            List<String> loreRaw = cfg == null ? null : cfg.getStringList(PATH_ROOT + "." + typeKey + ".lore");
            if (loreRaw != null && !loreRaw.isEmpty()) {
                List<String> lore = new ArrayList<>(loreRaw.size());
                String expires = formatExpires(expiresAtMillis);
                String usesText = formatUses(uses);
                for (String line : loreRaw) {
                    if (line == null) {
                        continue;
                    }
                    if (hideExpires && line.contains("{expires}")) {
                        continue;
                    }
                    lore.add(ColorUtil.colorize(line.replace("{expires}", expires).replace("{uses}", usesText)));
                }
                meta.setLore(lore);
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyType, PersistentDataType.STRING, type.name());
            pdc.set(keyExpiresAt, PersistentDataType.LONG, expiresAtMillis);
            pdc.set(keyToolId, PersistentDataType.STRING, toolId.toString());
            pdc.set(keyUses, PersistentDataType.INTEGER, uses);
            pdc.set(keyHideExpires, PersistentDataType.BYTE, (byte) (hideExpires ? 1 : 0));
            pdc.set(keyLastUsedBy, PersistentDataType.STRING, "");
            pdc.set(keySoldItems, PersistentDataType.INTEGER, 0);
            pdc.set(keyMoneyMade, PersistentDataType.DOUBLE, 0.0D);

            item.setItemMeta(meta);

            applyEnchantmentsFromConfig(item, type);

            if (registry != null) {
                registry.register(toolId, type, expiresAtMillis);
            }
        }

        return item;
    }

    public boolean shouldHideExpires(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte v = meta.getPersistentDataContainer().get(keyHideExpires, PersistentDataType.BYTE);
        return v != null && v != 0;
    }

    public boolean isTool(ItemStack item, SellToolType type) {
        SellToolType t = getToolType(item);
        return t == type;
    }

    public SellToolType getToolType(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SellToolType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String getLastUsedBy(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(keyLastUsedBy, PersistentDataType.STRING);
    }

    public int getSoldItems(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer v = meta.getPersistentDataContainer().get(keySoldItems, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public double getMoneyMade(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0D;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0.0D;
        }
        Double v = meta.getPersistentDataContainer().get(keyMoneyMade, PersistentDataType.DOUBLE);
        return v == null ? 0.0D : v;
    }

    public void recordSellUse(ItemStack item, String playerName, int soldAmount, double moneyMade) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyLastUsedBy, PersistentDataType.STRING, playerName);
        pdc.set(keySoldItems, PersistentDataType.INTEGER, getSoldItems(item) + soldAmount);
        pdc.set(keyMoneyMade, PersistentDataType.DOUBLE, getMoneyMade(item) + moneyMade);
        item.setItemMeta(meta);

        SellToolType type = getToolType(item);
        if (type != null) {
            refreshLore(item, type);
        }
    }

    public long getExpiresAt(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0L;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0L;
        }
        Long v = meta.getPersistentDataContainer().get(keyExpiresAt, PersistentDataType.LONG);
        return v == null ? 0L : v;
    }

    public boolean isExpired(ItemStack item) {
        long expiresAt = getExpiresAt(item);
        if (expiresAt <= 0L) {
            return true;
        }
        return System.currentTimeMillis() > expiresAt;
    }

    public int getUses(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return -1;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }
        Integer v = meta.getPersistentDataContainer().get(keyUses, PersistentDataType.INTEGER);
        return v == null ? -1 : v;
    }

    public boolean hasUses(ItemStack item) {
        int uses = getUses(item);
        return uses != 0;
    }

    public void decrementUses(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer v = pdc.get(keyUses, PersistentDataType.INTEGER);
        if (v == null) {
            return;
        }
        if (v < 0) {
            return;
        }
        int next = Math.max(0, v - 1);
        pdc.set(keyUses, PersistentDataType.INTEGER, next);
        item.setItemMeta(meta);

        SellToolType type = getToolType(item);
        if (type != null) {
            refreshLore(item, type);
        }
    }

    public void setUses(ItemStack item, int uses) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);

        SellToolType type = getToolType(item);
        if (type != null) {
            refreshLore(item, type);
        }
    }

    public void consumeOne(ItemStack item) {
        if (item == null) {
            return;
        }
        int amt = item.getAmount();
        if (amt <= 1) {
            item.setAmount(0);
            return;
        }
        item.setAmount(amt - 1);
    }

    public boolean isEnabled(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return false;
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getBoolean(PATH_ROOT + "." + typeKey + ".enabled", true);
    }

    public String getActivation(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return null;
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getString(PATH_ROOT + "." + typeKey + ".activation", type == SellToolType.AXE ? "BREAK" : "RIGHT_CLICK_BLOCK");
    }

    public boolean shouldSelfDestructOnExpiry(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return true;
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getBoolean(PATH_ROOT + "." + typeKey + ".self_destruct_on_expiry", true);
    }

    public int getDefaultTime(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return 1;
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getInt(PATH_ROOT + "." + typeKey + ".default_time", 1);
    }

    public String getDefaultUnit(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return "days";
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getString(PATH_ROOT + "." + typeKey + ".default_unit", "days");
    }

    public boolean shouldDestroyContainer(SellToolType type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return false;
        }
        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        return cfg.getBoolean(PATH_ROOT + "." + typeKey + ".destroy_container", false);
    }

    public boolean isAllowedContainer(Material type) {
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null || type == null) {
            return false;
        }
        List<String> list = cfg.getStringList("allowed_containers");
        if (list == null || list.isEmpty()) {
            return false;
        }
        String name = type.name();
        for (String s : list) {
            if (s != null && s.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public java.util.UUID getToolId(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(keyToolId, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean isActiveInRegistry(ItemStack item) {
        if (registry == null) {
            return true;
        }
        java.util.UUID id = getToolId(item);
        if (id == null) {
            return false;
        }

        if (registry.isActive(id)) {
            return true;
        }

        // If the tool is expired and self-destruction is disabled, we intentionally allow it
        // to remain in inventories so it can be recharged (e.g. via /sellwand setuses).
        SellToolType type = getToolType(item);
        return type != null && isExpired(item) && !shouldSelfDestructOnExpiry(type);
    }

    public void revoke(ItemStack item) {
        if (registry == null) {
            return;
        }
        java.util.UUID id = getToolId(item);
        if (id == null) {
            return;
        }
        registry.revoke(id);
    }

    public static String formatExpires(long expiresAtMillis) {
        if (expiresAtMillis <= 0L) {
            return "expired";
        }
        return EXPIRES_FMT.format(Instant.ofEpochMilli(expiresAtMillis));
    }

    public static String formatUses(int uses) {
        if (uses < 0) {
            return "Unlimited";
        }
        return Integer.toString(uses);
    }

    public void refreshLore(ItemStack item, SellToolType type) {
        if (item == null || item.getType().isAir() || type == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return;
        }

        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        List<String> loreRaw = cfg.getStringList(PATH_ROOT + "." + typeKey + ".lore");
        if (loreRaw == null || loreRaw.isEmpty()) {
            return;
        }

        long expiresAtMillis = getExpiresAt(item);
        int uses = getUses(item);
        boolean hideExpires = shouldHideExpires(item);

        String toolId = null;
        java.util.UUID id = getToolId(item);
        if (id != null) {
            toolId = id.toString();
        }
        String toolIdShort = null;
        if (toolId != null && toolId.length() >= 8) {
            toolIdShort = toolId.substring(0, 8);
        }
        String lastUsedBy = getLastUsedBy(item);
        if (lastUsedBy == null || lastUsedBy.isBlank()) {
            lastUsedBy = "NOT USED YET";
        }
        int soldItems = getSoldItems(item);
        double moneyMade = getMoneyMade(item);

        List<String> lore = new ArrayList<>(loreRaw.size());
        String expires = formatExpires(expiresAtMillis);
        String usesText = formatUses(uses);
        for (String line : loreRaw) {
            if (line == null) {
                continue;
            }
            if (hideExpires && line.contains("{expires}")) {
                continue;
            }
            String next = line
                    .replace("{expires}", expires)
                    .replace("{uses}", usesText)
                    .replace("{last_used_by}", lastUsedBy)
                    .replace("{sold_items}", Integer.toString(soldItems))
                    .replace("{money_made}", SellService.formatMoney(moneyMade))
                    .replace("{id}", toolId == null ? "" : toolId)
                    .replace("{id_short}", toolIdShort == null ? "" : toolIdShort);
            lore.add(ColorUtil.colorize(next));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void applyEnchantmentsFromConfig(ItemStack item, SellToolType type) {
        if (item == null || item.getType().isAir() || type == null) {
            return;
        }
        YamlConfiguration cfg = plugin.getConfigManager().getSellToolsConfig();
        if (cfg == null) {
            return;
        }

        String typeKey = type == SellToolType.WAND ? "wand" : "axe";
        List<String> enchLines = cfg.getStringList(PATH_ROOT + "." + typeKey + ".enchantments");
        if (enchLines == null || enchLines.isEmpty()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        for (String raw : enchLines) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String[] parts = raw.trim().split(":", 2);
            String name = parts[0].trim();
            int level = 1;
            if (parts.length == 2) {
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    level = 1;
                }
            }

            Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase(java.util.Locale.ROOT)));
            if (ench == null) {
                ench = Enchantment.getByName(name.toUpperCase(java.util.Locale.ROOT));
            }
            if (ench == null) {
                continue;
            }

            meta.addEnchant(ench, Math.max(1, level), true);
        }

        item.setItemMeta(meta);
    }
}
