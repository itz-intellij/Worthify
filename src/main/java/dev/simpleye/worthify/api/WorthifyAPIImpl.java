package dev.simpleye.worthify.api;

import dev.simpleye.worthify.sell.SellProcessResult;
import dev.simpleye.worthify.sell.SellResult;
import dev.simpleye.worthify.sell.SellService;
import dev.simpleye.worthify.worth.WorthManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class WorthifyAPIImpl implements WorthifyAPI {

    private final WorthManager worthManager;
    private final SellService sellService;

    public WorthifyAPIImpl(WorthManager worthManager, SellService sellService) {
        this.worthManager = worthManager;
        this.sellService = sellService;
    }

    @Override
    public double getUnitPrice(Material material) {
        return worthManager == null ? 0.0D : worthManager.getUnitPrice(material);
    }

    @Override
    public Map<Material, Double> getPricesSnapshot() {
        return worthManager == null ? java.util.Collections.emptyMap() : worthManager.getPricesSnapshot();
    }

    @Override
    public SellResult sellHand(Player player) {
        return sellService == null ? SellResult.nothingToSell() : sellService.sellHand(player, SellSource.API);
    }

    @Override
    public SellResult sellAll(Player player) {
        return sellService == null ? SellResult.nothingToSell() : sellService.sellAll(player, SellSource.API);
    }

    @Override
    public SellResult sellFromInventorySlots(Player player, Inventory inventory, List<Integer> slots) {
        return sellService == null ? SellResult.nothingToSell() : sellService.sellFromInventorySlots(player, inventory, slots, SellSource.API);
    }

    @Override
    public SellProcessResult sellAllFromInventory(Player player, Inventory inventory) {
        return sellService == null ? new SellProcessResult(SellResult.nothingToSell(), java.util.Collections.emptyList()) : sellService.sellAllFromInventory(player, inventory, SellSource.API);
    }
}
