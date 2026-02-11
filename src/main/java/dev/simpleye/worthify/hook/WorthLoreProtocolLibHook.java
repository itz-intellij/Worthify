package dev.simpleye.worthify.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.ColorUtil;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class WorthLoreProtocolLibHook {

    private final WorthifyPlugin plugin;
    private final ProtocolManager protocolManager;

    private PacketAdapter windowItemsAdapter;
    private PacketAdapter setSlotAdapter;

    public WorthLoreProtocolLibHook(WorthifyPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        if (windowItemsAdapter != null || setSlotAdapter != null) {
            return;
        }

        Plugin self = plugin;

        windowItemsAdapter = new PacketAdapter(self, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!isEnabledFor(event)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                List<ItemStack> items = packet.getItemListModifier().read(0);
                if (items == null || items.isEmpty()) {
                    return;
                }

                List<ItemStack> next = new ArrayList<>(items.size());
                for (ItemStack stack : items) {
                    next.add(withWorthLore(stack));
                }

                packet.getItemListModifier().write(0, next);
            }
        };

        setSlotAdapter = new PacketAdapter(self, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!isEnabledFor(event)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                ItemStack stack = packet.getItemModifier().read(0);
                packet.getItemModifier().write(0, withWorthLore(stack));
            }
        };

        protocolManager.addPacketListener(windowItemsAdapter);
        protocolManager.addPacketListener(setSlotAdapter);
    }

    public void stop() {
        if (windowItemsAdapter != null) {
            protocolManager.removePacketListener(windowItemsAdapter);
            windowItemsAdapter = null;
        }
        if (setSlotAdapter != null) {
            protocolManager.removePacketListener(setSlotAdapter);
            setSlotAdapter = null;
        }
    }

    private boolean isEnabledFor(PacketEvent event) {
        if (event == null || event.getPlayer() == null) {
            return false;
        }

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (!cfg.getBoolean("worth_lore.enabled", false)) {
            return false;
        }

        if (cfg.getBoolean("worth_lore.require_protocollib", true)) {
            if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                return false;
            }
        }

        return true;
    }

    private ItemStack withWorthLore(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return stack;
        }

        Material type = stack.getType();
        double unit = plugin.getWorthManager().getUnitPrice(type);
        double total = plugin.applyWorthMultiplier(type, unit * stack.getAmount());

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        boolean allowUnsellable = cfg.getBoolean("worth_lore.add_to_unsellable_items", false);
        if (!allowUnsellable && total <= 0.0D) {
            return stack;
        }

        if (total <= 0.0D) {
            return stack;
        }

        String template = cfg.getString("worth_lore.line", "&7Worth: &a${worth}");
        String worthText = SellService.formatMoney(total);
        String line = ColorUtil.colorize(template
                .replace("${worth}", worthText)
                .replace("{worth}", worthText));

        String marker = extractMarker(template);

        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return stack;
        }

        List<String> lore = meta.getLore();
        List<String> nextLore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    // alr here we go
        if (marker != null && !marker.isEmpty()) {
            for (int i = 0; i < nextLore.size(); i++) {
                String existing = nextLore.get(i);
                String stripped = ChatColor.stripColor(existing);
                if (stripped != null && stripped.trim().toLowerCase().startsWith(marker)) {
                    nextLore.set(i, line);
                    meta.setLore(nextLore);
                    copy.setItemMeta(meta);
                    return copy;
                }
            }
        }

        nextLore.add(line);
        meta.setLore(nextLore);
        copy.setItemMeta(meta);
        return copy;
    }

    private static String extractMarker(String templateOrLine) {
        if (templateOrLine == null) {
            return null;
        }

        int placeholder = templateOrLine.indexOf("{worth}");
        if (placeholder == -1) {
            placeholder = templateOrLine.indexOf("${worth}");
        }
        if (placeholder == -1) {
            return null;
        }

        String left = templateOrLine.substring(0, placeholder);
        String colored = ColorUtil.colorize(left);
        String stripped = ChatColor.stripColor(colored);
        if (stripped == null) {
            return null;
        }
        return stripped.trim().toLowerCase();
    }
}
