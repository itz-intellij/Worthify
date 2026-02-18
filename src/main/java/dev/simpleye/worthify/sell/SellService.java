package dev.simpleye.worthify.sell;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.api.SellSource;
import dev.simpleye.worthify.api.WorthifySellEvent;
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
import java.util.function.ToDoubleFunction;

public final class SellService {

    private static final int MAX_CONTAINER_DEPTH = 4;

    private final WorthManager worthManager;
    private final EconomyHook economyHook;
    private final SellHistoryStore historyStore;
    private final ToDoubleFunction<Material> worthMultiplier;
    private final WorthifyPlugin plugin;

    public SellService(WorthifyPlugin plugin, WorthManager worthManager, EconomyHook economyHook, SellHistoryStore historyStore) {
        this(plugin, worthManager, economyHook, historyStore, m -> 1.0D);
    }

    public SellService(WorthifyPlugin plugin, WorthManager worthManager, EconomyHook economyHook, SellHistoryStore historyStore, ToDoubleFunction<Material> worthMultiplier) {
        this.plugin = plugin;
        this.worthManager = worthManager;
        this.economyHook = economyHook;
        this.historyStore = historyStore;
        this.worthMultiplier = worthMultiplier == null ? m -> 1.0D : worthMultiplier;
    }

    public SellResult sellHand(Player player) {
        return sellHand(player, SellSource.OTHER);
    }

    public SellResult sellHand(Player player, SellSource source) {
        if (!economyHook.isEnabled()) {
            SellResult r = SellResult.disabledEconomy();
            fireSellEvent(player, r, 0, source);
            return r;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, 0, source);
            return r;
        }

        double total = calculateStackWorth(item, 0);
        if (!(total > 0.0D) || Double.isNaN(total) || Double.isInfinite(total)) {
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, 0, source);
            return r;
        }

        int amount = item.getAmount();

        inv.setItemInMainHand(new ItemStack(Material.AIR));
        economyHook.deposit(player, total);

        if (historyStore != null) {
            historyStore.append(player.getUniqueId(), new SellHistoryEntry(System.currentTimeMillis(), item.getType().name(), amount, total));
        }

        SellResult r = SellResult.success(amount, total);
        fireSellEvent(player, r, 0, source);
        return r;
    }

    public SellResult sellAll(Player player) {
        return sellAll(player, SellSource.OTHER);
    }

    public SellResult sellAll(Player player, SellSource source) {
        if (!economyHook.isEnabled()) {
            SellResult r = SellResult.disabledEconomy();
            fireSellEvent(player, r, 0, source);
            return r;
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
            if (!(stackWorth > 0.0D) || Double.isNaN(stackWorth) || Double.isInfinite(stackWorth)) {
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
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, 0, source);
            return r;
        }

        if (!(total > 0.0D) || Double.isNaN(total) || Double.isInfinite(total)) {
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, 0, source);
            return r;
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

        SellResult r = SellResult.success(soldAmount, total);
        fireSellEvent(player, r, 0, source);
        return r;
    }

    public SellResult sellFromInventorySlots(Player player, Inventory inventory, List<Integer> slots) {
        return sellFromInventorySlots(player, inventory, slots, SellSource.OTHER);
    }

    public SellResult sellFromInventorySlots(Player player, Inventory inventory, List<Integer> slots, SellSource source) {
        if (!economyHook.isEnabled()) {
            SellResult r = SellResult.disabledEconomy();
            fireSellEvent(player, r, 0, source);
            return r;
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
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, 0, source);
            return r;
        }

        economyHook.deposit(player, total);

        if (historyStore != null) {
            long ts = System.currentTimeMillis();
            for (Map.Entry<Material, SoldBucket> e : soldByType.entrySet()) {
                SoldBucket b = e.getValue();
                historyStore.append(player.getUniqueId(), new SellHistoryEntry(ts, e.getKey().name(), b.amount, b.total));
            }
        }

        SellResult r = SellResult.success(soldAmount, total);
        fireSellEvent(player, r, 0, source);
        return r;
    }

    public SellProcessResult sellAllFromInventory(Player player, Inventory inventory) {
        return sellAllFromInventory(player, inventory, SellSource.OTHER);
    }

    public SellProcessResult sellAllFromInventory(Player player, Inventory inventory, SellSource source) {
        if (!economyHook.isEnabled()) {
            List<ItemStack> all = new ArrayList<>();
            for (ItemStack item : inventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    all.add(item);
                }
            }
            inventory.clear();
            SellResult r = SellResult.disabledEconomy();
            fireSellEvent(player, r, all.size(), source);
            return new SellProcessResult(r, all);
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
            SellResult r = SellResult.nothingToSell();
            fireSellEvent(player, r, unsellable.size(), source);
            return new SellProcessResult(r, unsellable);
        }

        economyHook.deposit(player, total);

        if (historyStore != null) {
            long ts = System.currentTimeMillis();
            for (Map.Entry<Material, SoldBucket> e : soldByType.entrySet()) {
                SoldBucket b = e.getValue();
                historyStore.append(player.getUniqueId(), new SellHistoryEntry(ts, e.getKey().name(), b.amount, b.total));
            }
        }

        SellResult r = SellResult.success(soldAmount, total);
        fireSellEvent(player, r, unsellable.size(), source);
        return new SellProcessResult(r, unsellable);
    }

    private void fireSellEvent(Player player, SellResult result, int unsellableCount, SellSource source) {
        if (plugin == null) {
            return;
        }
        if (player == null || result == null) {
            return;
        }
        try {
            plugin.getServer().getPluginManager().callEvent(new WorthifySellEvent(player, result, unsellableCount, source == null ? SellSource.OTHER : source));
        } catch (Throwable ignored) {
            // ignore
        }
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

        Material type = stack.getType();
        double perItem = worthManager.getUnitPrice(type);
        perItem += calculateContainerBonusPerItem(stack, depth);
        if (perItem <= 0.0D) {
            return 0.0D;
        }

        double mult = getMultiplier(type);
        double total = (perItem * mult) * stack.getAmount();
        if (!(total > 0.0D) || Double.isNaN(total) || Double.isInfinite(total)) {
            return 0.0D;
        }
        return total;
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

    private double getMultiplier(Material material) {
        double mult;
        try {
            mult = worthMultiplier.applyAsDouble(material);
        } catch (Exception ignored) {
            mult = 1.0D;
        }
        if (!(mult > 0.0D) || Double.isNaN(mult) || Double.isInfinite(mult)) {
            return 1.0D;
        }
        return mult;
    }

    public static String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
