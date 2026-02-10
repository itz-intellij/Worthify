package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.gui.TopBalGuiHolder;
import dev.simpleye.worthify.gui.TopBalGuiManager;
import dev.simpleye.worthify.gui.TopBalGuiSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class TopBalGuiListener implements Listener {

    private final TopBalGuiManager gui;

    public TopBalGuiListener(TopBalGuiManager gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TopBalGuiHolder)) {
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

        TopBalGuiSession.State state = TopBalGuiSession.get(player);
        if (state == null) {
            return;
        }

        if (rawSlot == TopBalGuiManager.BACK_SLOT && state.page() > 1) {
            gui.open(player, state.page() - 1, state.limit());
        } else if (rawSlot == TopBalGuiManager.NEXT_SLOT && state.page() < state.maxPages()) {
            gui.open(player, state.page() + 1, state.limit());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TopBalGuiHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            TopBalGuiSession.clear(player);
        }
    }
}
