package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.TopBalGuiManager;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TopBalanceCommand implements CommandExecutor {

    private static final int MAX_VAULT_SCAN = 5000;

    private final WorthifyPlugin plugin;
    private final TopBalGuiManager gui;

    public TopBalanceCommand(WorthifyPlugin plugin, TopBalGuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();
        if (!plugin.getEconomyHook().isEnabled()) {
            if (messages != null) {
                messages.send(sender, "errors.economy_unavailable");
            } else {
                sender.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return true;
        }

        int page = 1;
        int limit = 10;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "topbal.usage_page");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /baltop [page]");
                }
                return true;
            }
        } else if (args.length == 2) {
            try {
                page = Integer.parseInt(args[0]);
                limit = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "topbal.usage_page_limit");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /baltop [page] [limit]");
                }
                return true;
            }
        }

        if (limit <= 0) {
            limit = 10;
        }
        if (page <= 0) {
            page = 1;
        }
        int cappedLimit = Math.max(1, Math.min(100, limit));

        if (sender instanceof Player player && gui != null) {
            gui.open(player, page, cappedLimit);
            return true;
        }

        if (plugin.getEconomyHook().isUsingInternalEconomy()) {
            sendInternalTopBalances(sender, page, cappedLimit);
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Calculating top balances...");
        int finalPage = page;
        int finalLimit = cappedLimit;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<String, Double>> top = computeVaultTopBalances(finalPage, finalLimit);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "Top Balances:");
                if (top.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No balances found.");
                    return;
                }

                int startIndex = (finalPage - 1) * finalLimit;
                for (int i = 0; i < top.size(); i++) {
                    int rank = startIndex + i + 1;
                    Map.Entry<String, Double> e = top.get(i);
                    sender.sendMessage(ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + e.getKey() + ChatColor.GRAY + " - " + ChatColor.AQUA + "$" + SellService.formatMoney(e.getValue()));
                }
            });
        });

        return true;
    }

    private void sendInternalTopBalances(CommandSender sender, int page, int limit) {
        List<Map.Entry<UUID, Double>> all = plugin.getEconomyHook().topInternalBalances(Integer.MAX_VALUE);
        int from = Math.max(0, (page - 1) * limit);
        if (from >= all.size()) {
            sender.sendMessage(ChatColor.GOLD + "Top Balances:");
            sender.sendMessage(ChatColor.GRAY + "No balances yet.");
            return;
        }
        int to = Math.min(all.size(), from + limit);

        sender.sendMessage(ChatColor.GOLD + "Top Balances:");
        for (int i = from; i < to; i++) {
            Map.Entry<UUID, Double> e = all.get(i);
            OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
            String name = p.getName() == null ? e.getKey().toString() : p.getName();
            int rank = i + 1;
            sender.sendMessage(ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + name + ChatColor.GRAY + " - " + ChatColor.AQUA + "$" + SellService.formatMoney(e.getValue()));
        }
    }

    private List<Map.Entry<String, Double>> computeVaultTopBalances(int page, int limit) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        int cap = Math.min(players.length, MAX_VAULT_SCAN);
        List<Map.Entry<String, Double>> list = new ArrayList<>(cap);

        for (int i = 0; i < cap; i++) {
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

        int from = Math.max(0, (page - 1) * limit);
        if (from >= list.size()) {
            return java.util.Collections.emptyList();
        }
        int to = Math.min(list.size(), from + limit);
        return new ArrayList<>(list.subList(from, to));
    }
}
