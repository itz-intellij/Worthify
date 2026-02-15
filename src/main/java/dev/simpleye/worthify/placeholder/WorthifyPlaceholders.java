package dev.simpleye.worthify.placeholder;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.util.MoneyUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorthifyPlaceholders extends PlaceholderExpansion {

    private final WorthifyPlugin plugin;

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

        return null;
    }
}
