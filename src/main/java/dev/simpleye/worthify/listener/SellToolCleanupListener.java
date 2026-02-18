package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.selltools.SellToolType;
import dev.simpleye.worthify.selltools.SellToolUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SellToolCleanupListener implements Listener {

    private final WorthifyPlugin plugin;
    private final SellToolUtil util;

    public SellToolCleanupListener(WorthifyPlugin plugin, SellToolUtil util) {
        this.plugin = plugin;
        this.util = util;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        cleanupInventory(player.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) {
            return;
        }
        cleanupInventory(inv);
    }

    private void cleanupInventory(Inventory inv) {
        if (inv == null) {
            return;
        }

        ItemStack[] contents = inv.getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            SellToolType type = util.getToolType(item);
            if (type == null) {
                continue;
            }

            boolean expired = util.isExpired(item);
            boolean active = util.isActiveInRegistry(item);
            if (!expired && active) {
                continue;
            }

            util.revoke(item);
            contents[i] = null;
            changed = true;
        }

        if (changed) {
            inv.setContents(contents);
        }
    }
}
