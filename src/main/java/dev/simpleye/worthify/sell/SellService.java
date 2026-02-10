package dev.simpleye.worthify.sell;

import dev.simpleye.worthify.history.SellHistoryEntry;
import dev.simpleye.worthify.history.SellHistoryStore;
import dev.simpleye.worthify.economy.EconomyHook;
import dev.simpleye.worthify.worth.WorthManager;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class SellService {

    private static final int MAX_CONTAINER_DEPTH = 4;

    private final WorthManager worthManager;
    private final EconomyHook economyHook;
    private final SellHistoryStore historyStore;

    public SellService(WorthManager worthManager, EconomyHook economyHook, SellHistoryStore historyStore) {
        this.worthManager = worthManager;
        this.economyHook = economyHook;
        this.historyStore = historyStore;
    }

    public SellResult sellHand(Player player) {
        if (!economyHook.isEnabled()) {
            return SellResult.disabledEconomy();
        }

        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return SellResult.nothingToSell();
        }

        double total = calculateStackWorth(item, 0);
        if (total <= 0.0D) {
            return SellResult.nothingToSell();
        }

        int amount = item.getAmount();

        inv.setItemInMainHand(new ItemStack(Material.AIR));
        economyHook.deposit(player, total);

        if (historyStore != null) {
            historyStore.append(player.getUniqueId(), new SellHistoryEntry(System.currentTimeMillis(), item.getType().name(), amount, total));
        }

        return SellResult.success(amount, total);
    }

    public SellResult sellAll(Player player) {
        if (!economyHook.isEnabled()) {
            return SellResult.disabledEconomy();
        }

        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();

        int soldAmount = 0;
        double total = 0.0D;

        Map<Material, SoldBucket> soldByType = new HashMap<>();

        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            double stackWorth = calculateStackWorth(item, 0);
            if (stackWorth <= 0.0D) {
                continue;
            }

            int amount = item.getAmount();
            soldAmount += amount;
            total += stackWorth;

            SoldBucket bucket = soldByType.computeIfAbsent(item.getType(), k -> new SoldBucket());
            bucket.amount += amount;
            bucket.total += stackWorth;

            storage[i] = null;
        }

        if (soldAmount <= 0) {
            return SellResult.nothingToSell();
        }

        inv.setStorageContents(storage);
        economyHook.deposit(player, total);

        if (historyStore != null) {
            long ts = System.currentTimeMillis();
            for (Map.Entry<Material, SoldBucket> e : soldByType.entrySet()) {
                SoldBucket b = e.getValue();
                historyStore.append(player.getUniqueId(), new SellHistoryEntry(ts, e.getKey().name(), b.amount, b.total));
            }
        }

        return SellResult.success(soldAmount, total);
    }

    public SellResult sellFromInventorySlots(Player player, Inventory inventory, List<Integer> slots) {
        if (!economyHook.isEnabled()) {
            return SellResult.disabledEconomy();
        }

        int soldAmount = 0;
        double total = 0.0D;

        Map<Material, SoldBucket> soldByType = new HashMap<>();

        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }

            double stackWorth = calculateStackWorth(item, 0);
            if (stackWorth <= 0.0D) {
                continue;
            }

            int amount = item.getAmount();
            soldAmount += amount;
            total += stackWorth;

            SoldBucket bucket = soldByType.computeIfAbsent(item.getType(), k -> new SoldBucket());
            bucket.amount += amount;
            bucket.total += stackWorth;

            inventory.setItem(slot, null);
        }

        if (soldAmount <= 0) {
            return SellResult.nothingToSell();
        }

        economyHook.deposit(player, total);

        if (historyStore != null) {
            long ts = System.currentTimeMillis();
            for (Map.Entry<Material, SoldBucket> e : soldByType.entrySet()) {
                SoldBucket b = e.getValue();
                historyStore.append(player.getUniqueId(), new SellHistoryEntry(ts, e.getKey().name(), b.amount, b.total));
            }
        }
        return SellResult.success(soldAmount, total);
    }

    public SellProcessResult sellAllFromInventory(Player player, Inventory inventory) {
        if (!economyHook.isEnabled()) {
            List<ItemStack> all = new ArrayList<>();
            for (ItemStack item : inventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    all.add(item);
                }
            }
            inventory.clear();
            return new SellProcessResult(SellResult.disabledEconomy(), all);
        }

        int soldAmount = 0;
        double total = 0.0D;
        List<ItemStack> unsellable = new ArrayList<>();

        Map<Material, SoldBucket> soldByType = new HashMap<>();

        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            double stackWorth = calculateStackWorth(item, 0);
            if (stackWorth <= 0.0D) {
                unsellable.add(item);
                continue;
            }

            int amount = item.getAmount();
            soldAmount += amount;
            total += stackWorth;

            SoldBucket bucket = soldByType.computeIfAbsent(item.getType(), k -> new SoldBucket());
            bucket.amount += amount;
            bucket.total += stackWorth;
        }

        inventory.clear();

        if (soldAmount <= 0) {
            return new SellProcessResult(SellResult.nothingToSell(), unsellable);
        }

        economyHook.deposit(player, total);

        if (historyStore != null) {
            long ts = System.currentTimeMillis();
            for (Map.Entry<Material, SoldBucket> e : soldByType.entrySet()) {
                SoldBucket b = e.getValue();
                historyStore.append(player.getUniqueId(), new SellHistoryEntry(ts, e.getKey().name(), b.amount, b.total));
            }
        }

        return new SellProcessResult(SellResult.success(soldAmount, total), unsellable);
    }

    private static final class SoldBucket {
        private int amount;
        private double total;
    }

    private double calculateStackWorth(ItemStack stack, int depth) {
        if (stack == null || stack.getType().isAir()) {
            return 0.0D;
        }
        if (depth > MAX_CONTAINER_DEPTH) {
            return 0.0D;
        }

        double perItem = worthManager.getUnitPrice(stack.getType());
        perItem += calculateContainerBonusPerItem(stack, depth);
        if (perItem <= 0.0D) {
            return 0.0D;
        }
        return perItem * stack.getAmount();
    }

    private double calculateContainerBonusPerItem(ItemStack stack, int depth) {
        if (depth >= MAX_CONTAINER_DEPTH) {
            return 0.0D;
        }

        if (!(stack.getItemMeta() instanceof BlockStateMeta meta)) {
            return 0.0D;
        }

        if (!(meta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return 0.0D;
        }

        double contentsWorth = 0.0D;
        for (ItemStack inside : shulkerBox.getInventory().getContents()) {
            contentsWorth += calculateStackWorth(inside, depth + 1);
        }
        return contentsWorth;
    }

    public static String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
