package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.gui.SellHistoryGuiHolder;
import dev.simpleye.worthify.gui.SellHistoryGuiManager;
import dev.simpleye.worthify.gui.SellHistoryGuiSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class SellHistoryGuiListener implements Listener {

    private final SellHistoryGuiManager gui;

    public SellHistoryGuiListener(SellHistoryGuiManager gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellHistoryGuiHolder)) {
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

        SellHistoryGuiSession.State state = SellHistoryGuiSession.get(player);
        if (state == null) {
            return;
        }

        if (rawSlot == SellHistoryGuiManager.BACK_SLOT && state.page() > 1) {
            gui.open(player, state.page() - 1);
        } else if (rawSlot == SellHistoryGuiManager.NEXT_SLOT && state.page() < state.maxPages()) {
            gui.open(player, state.page() + 1);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellHistoryGuiHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            SellHistoryGuiSession.clear(player);
        }
    }
}
