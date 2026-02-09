package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.WorthGuiHolder;
import dev.simpleye.worthify.gui.WorthGuiManager;
import dev.simpleye.worthify.gui.WorthGuiSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class WorthGuiListener implements Listener {

    private final WorthifyPlugin plugin;
    private final WorthGuiManager worthGuiManager;

    public WorthGuiListener(WorthifyPlugin plugin, WorthGuiManager worthGuiManager) {
        this.plugin = plugin;
        this.worthGuiManager = worthGuiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthGuiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= event.getInventory().getSize()) {
            return;
        }

        WorthGuiSession.State state = WorthGuiSession.get(player);
        if (state == null) {
            return;
        }

        if (rawSlot == WorthGuiManager.BACK_SLOT && state.page() > 1) {
            worthGuiManager.open(player, state.page() - 1);
        } else if (rawSlot == WorthGuiManager.NEXT_SLOT && state.page() < state.maxPages()) {
            worthGuiManager.open(player, state.page() + 1);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthGuiHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            WorthGuiSession.clear(player);
        }
    }
}
