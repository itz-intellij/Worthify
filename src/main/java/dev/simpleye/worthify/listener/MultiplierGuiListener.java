package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.MultiplierGuiHolder;
import dev.simpleye.worthify.gui.MultiplierGuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MultiplierGuiListener implements Listener {

    private final WorthifyPlugin plugin;
    private final MultiplierGuiManager gui;

    public MultiplierGuiListener(WorthifyPlugin plugin, MultiplierGuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MultiplierGuiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.isWorthMultiplierEnabled()) {
            player.closeInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= event.getInventory().getSize()) {
            return;
        }

        String category = switch (rawSlot) {
            case MultiplierGuiManager.SLOT_ORES -> "ores";
            case MultiplierGuiManager.SLOT_FOOD -> "food";
            case MultiplierGuiManager.SLOT_SEEDS -> "seeds";
            default -> null;
        };

        if (category == null) {
            return;
        }

        if (!player.hasPermission("worthify.admin")) {
            return;
        }

        ClickType click = event.getClick();
        boolean shift = click.isShiftClick();

        double step = shift ? 1.0D : 0.1D;
        boolean add = click.isLeftClick();
        boolean sub = click.isRightClick();

        if (!add && !sub) {
            return;
        }

        double current = plugin.getConfiguredCategoryMultiplier(category);
        double next = add ? current + step : current - step;
        if (next <= 0.0D) {
            next = 0.01D;
        }

        plugin.setConfiguredCategoryMultiplier(category, next);
        gui.refresh(player);
    }
}
