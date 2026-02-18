package dev.simpleye.worthify.api;

import dev.simpleye.worthify.sell.SellProcessResult;
import dev.simpleye.worthify.sell.SellResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public interface WorthifyAPI {

    double getUnitPrice(Material material);

    Map<Material, Double> getPricesSnapshot();

    SellResult sellHand(Player player);

    SellResult sellAll(Player player);

    SellResult sellFromInventorySlots(Player player, Inventory inventory, List<Integer> slots);

    SellProcessResult sellAllFromInventory(Player player, Inventory inventory);
}
