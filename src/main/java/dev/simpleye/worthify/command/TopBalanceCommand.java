package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TopBalanceCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public TopBalanceCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getEconomyHook().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Economy is not available.");
            return true;
        }

        if (!plugin.getEconomyHook().isUsingInternalEconomy()) {
            sender.sendMessage(ChatColor.RED + "Top balances is only available when using Worthify internal economy.");
            return true;
        }

        int limit = 10;
        if (args.length == 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Usage: /topbal [limit]");
                return true;
            }
        }

        List<Map.Entry<UUID, Double>> top = plugin.getEconomyHook().topInternalBalances(limit);
        sender.sendMessage(ChatColor.GOLD + "Top Balances:");

        int i = 1;
        for (Map.Entry<UUID, Double> e : top) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
            String name = p.getName() == null ? e.getKey().toString() : p.getName();
            sender.sendMessage(ChatColor.YELLOW + "#" + i + " " + ChatColor.WHITE + name + ChatColor.GRAY + " - " + ChatColor.AQUA + "$" + SellService.formatMoney(e.getValue()));
            i++;
        }

        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No balances yet.");
        }

        return true;
    }
}
