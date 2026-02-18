package dev.simpleye.worthify.selltools;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    private static final DateTimeFormatter EXPIRES_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public SellToolUtil(WorthifyPlugin plugin, SellToolRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.keyType = new NamespacedKey(plugin, "sell_tool_type");
        this.keyExpiresAt = new NamespacedKey(plugin, "sell_tool_expires_at");
        this.keyToolId = new NamespacedKey(plugin, "sell_tool_id");
    }

    public ItemStack createTool(SellToolType type, long expiresAtMillis) {
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
                for (String line : loreRaw) {
                    if (line == null) {
                        continue;
                    }
                    lore.add(ColorUtil.colorize(line.replace("{expires}", expires)));
                }
                meta.setLore(lore);
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyType, PersistentDataType.STRING, type.name());
            pdc.set(keyExpiresAt, PersistentDataType.LONG, expiresAtMillis);
            pdc.set(keyToolId, PersistentDataType.STRING, toolId.toString());

            item.setItemMeta(meta);

            if (registry != null) {
                registry.register(toolId, type, expiresAtMillis);
            }
        }

        return item;
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
        return registry.isActive(id);
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
}
