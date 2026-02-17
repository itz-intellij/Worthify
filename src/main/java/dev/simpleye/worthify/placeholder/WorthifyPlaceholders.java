package dev.simpleye.worthify.placeholder;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.util.MoneyUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorthifyPlaceholders extends PlaceholderExpansion {

    private static final int MAX_VAULT_SCAN = 5000;
    private static final long VAULT_CACHE_TTL_MILLIS = 60_000L;

    private final WorthifyPlugin plugin;

    private volatile List<Map.Entry<String, Double>> topVaultCache = Collections.emptyList();
    private volatile long topVaultCacheTimeMillis;
    private volatile boolean topVaultComputing;

    public WorthifyPlaceholders(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "worthify";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YE";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params == null) {
            return "";
        }

        String id = params.toLowerCase(java.util.Locale.ROOT);

        if (id.equals("balance")) {
            if (player == null || plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
                return "0.00";
            }
            return MoneyUtil.formatShort(plugin.getEconomyHook().getBalance(player));
        }

        if (id.equals("balance_formatted")) {
            if (player == null || plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
                return "0.00";
            }
            return MoneyUtil.format(plugin.getEconomyHook().getBalance(player));
        }

        if (id.equals("balance_plain")) {
            if (player == null || plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
                return "0.00";
            }
            return MoneyUtil.formatPlain(plugin.getEconomyHook().getBalance(player));
        }

        if (id.equals("balance_short")) {
            if (player == null || plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
                return "0.00";
            }
            return MoneyUtil.formatShort(plugin.getEconomyHook().getBalance(player));
        }

        if (id.equals("economy")) {
            if (plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
                return "disabled";
            }
            return plugin.getEconomyHook().isUsingInternalEconomy() ? "internal" : "vault";
        }

        if (id.startsWith("top_")) {
            return handleTopPlaceholder(id);
        }

        return null;
    }

    private String handleTopPlaceholder(String id) {
        if (plugin.getEconomyHook() == null || !plugin.getEconomyHook().isEnabled()) {
            return "";
        }

        // AjLeaderboards-friendly placeholders:
        // %worthify_top_name_1%
        // %worthify_top_balance_1%
        // %worthify_top_balance_formatted_1%
        // %worthify_top_balance_short_1%
        int lastUnderscore = id.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == id.length() - 1) {
            return "";
        }
        String indexPart = id.substring(lastUnderscore + 1);
        int index;
        try {
            index = Integer.parseInt(indexPart);
        } catch (NumberFormatException ex) {
            return "";
        }
        if (index <= 0) {
            return "";
        }

        String base = id.substring(0, lastUnderscore);
        List<Map.Entry<String, Double>> top = getTopBalancesCached(Math.max(10, index));
        if (top.isEmpty() || index > top.size()) {
            return "";
        }

        Map.Entry<String, Double> entry = top.get(index - 1);
        String name = entry.getKey() == null ? "" : entry.getKey();
        double bal = entry.getValue() == null ? 0.0D : entry.getValue();

        return switch (base) {
            case "top_name" -> name;
            case "top_balance" -> MoneyUtil.formatPlain(bal);
            case "top_balance_formatted" -> MoneyUtil.format(bal);
            case "top_balance_short" -> MoneyUtil.formatShort(bal);
            default -> "";
        };
    }

    private List<Map.Entry<String, Double>> getTopBalancesCached(int limit) {
        int cap = Math.max(1, Math.min(100, limit));

        if (plugin.getEconomyHook().isUsingInternalEconomy()) {
            List<Map.Entry<UUID, Double>> list = plugin.getEconomyHook().topInternalBalances(cap);
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map.Entry<String, Double>> mapped = new ArrayList<>(Math.min(cap, list.size()));
            int to = Math.min(cap, list.size());
            for (int i = 0; i < to; i++) {
                Map.Entry<UUID, Double> e = list.get(i);
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                String name = p.getName() == null ? e.getKey().toString() : p.getName();
                mapped.add(Map.entry(name, e.getValue()));
            }
            return mapped;
        }

        long now = System.currentTimeMillis();
        boolean stale = topVaultCache.isEmpty() || (now - topVaultCacheTimeMillis) > VAULT_CACHE_TTL_MILLIS;
        if (stale && !topVaultComputing) {
            topVaultComputing = true;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Map.Entry<String, Double>> computed = computeVaultTopBalances(Integer.MAX_VALUE);
                topVaultCache = computed;
                topVaultCacheTimeMillis = System.currentTimeMillis();
                topVaultComputing = false;
            });
        }

        if (topVaultCache.isEmpty()) {
            return Collections.emptyList();
        }
        if (topVaultCache.size() > cap) {
            return new ArrayList<>(topVaultCache.subList(0, cap));
        }
        return topVaultCache;
    }

    private List<Map.Entry<String, Double>> computeVaultTopBalances(int limit) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        int scanCap = Math.min(players.length, MAX_VAULT_SCAN);
        List<Map.Entry<String, Double>> list = new ArrayList<>(scanCap);

        for (int i = 0; i < scanCap; i++) {
            OfflinePlayer p = players[i];
            if (p == null) {
                continue;
            }
            double bal = plugin.getEconomyHook().getBalance(p);
            if (bal <= 0.0D) {
                continue;
            }
            String name = p.getName();
            if (name == null || name.isBlank()) {
                name = p.getUniqueId().toString();
            }
            list.add(Map.entry(name, bal));
        }

        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (limit <= 0 || limit == Integer.MAX_VALUE) {
            return list;
        }
        int cap = Math.max(1, Math.min(100, limit));
        if (list.size() > cap) {
            return new ArrayList<>(list.subList(0, cap));
        }
        return list;
    }
}
