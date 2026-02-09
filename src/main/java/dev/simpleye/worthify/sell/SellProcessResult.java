package dev.simpleye.worthify.sell;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public record SellProcessResult(SellResult result, List<ItemStack> unsellable) {
}
