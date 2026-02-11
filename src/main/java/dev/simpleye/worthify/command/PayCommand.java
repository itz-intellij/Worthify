package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.compat.VanishHook;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PayCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    private final Map<UUID, Long> lastPayMillis = new ConcurrentHashMap<>();

    public PayCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        if (args.length != 2) {
            if (messages != null) {
                messages.send(sender, "pay.usage");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            }
            return true;
        }

        if (!plugin.getConfig().getBoolean("pay.enabled", true)) {
            if (messages != null) {
                messages.send(sender, "pay.disabled");
            } else {
                sender.sendMessage(ChatColor.RED + "Pay is disabled.");
            }
            return true;
        }

        long cooldownSeconds = plugin.getConfig().getLong("pay.cooldown_seconds", 0L);
        if (cooldownSeconds > 0L) {
            long now = System.currentTimeMillis();
            long last = lastPayMillis.getOrDefault(player.getUniqueId(), 0L);
            long elapsed = now - last;
            long cooldownMillis = cooldownSeconds * 1000L;
            if (elapsed < cooldownMillis) {
                long leftSec = Math.max(1L, (cooldownMillis - elapsed + 999L) / 1000L);
                if (messages != null) {
                    messages.send(sender, "pay.cooldown", "seconds", leftSec);
                } else {
                    sender.sendMessage(ChatColor.RED + "You must wait " + leftSec + "s before paying again.");
                }
                return true;
            }
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || VanishHook.isVanished(target)) {
            if (messages != null) {
                messages.send(sender, "pay.player_not_found", "player", args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found (must be online): " + args[0]);
            }
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            if (messages != null) {
                messages.send(sender, "pay.cannot_pay_self");
            } else {
                sender.sendMessage(ChatColor.RED + "You cannot pay yourself.");
            }
            return true;
        }

        Double parsed = MoneyUtil.parseAmount(args[1]);
        if (parsed == null) {
            if (messages != null) {
                messages.send(sender, "pay.invalid_amount", "amount", args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            }
            return true;
        }

        double amount = parsed;

        if (amount <= 0.0D) {
            if (messages != null) {
                messages.send(sender, "pay.amount_must_be_positive");
            } else {
                sender.sendMessage(ChatColor.RED + "Amount must be > 0");
            }
            return true;
        }

        boolean opBypass = plugin.getConfig().getBoolean("pay.receive_toggle.op_bypass", true);
        if (!opBypass || !player.isOp()) {
            boolean allowReceive = plugin.getPaySettingsStore() == null || plugin.getPaySettingsStore().isReceiveEnabled(target.getUniqueId());
            if (!allowReceive) {
                if (messages != null) {
                    messages.send(sender, "pay.target_disabled", "player", target.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + target.getName() + " is not accepting payments.");
                }
                return true;
            }
        }

        if (!plugin.getEconomyHook().isEnabled()) {
            if (messages != null) {
                messages.send(sender, "errors.economy_unavailable");
            } else {
                sender.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return true;
        }

        OfflinePlayer from = player;
        OfflinePlayer to = target;

        double balance = plugin.getEconomyHook().getBalance(from);
        if (balance < amount) {
            if (messages != null) {
                messages.send(sender, "pay.not_enough", "balance", MoneyUtil.format(balance));
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have enough money. Balance: $" + MoneyUtil.format(balance));
            }
            return true;
        }

        boolean ok = plugin.getEconomyHook().transfer(from, to, amount);
        if (!ok) {
            if (messages != null) {
                messages.send(sender, "pay.failed");
            } else {
                sender.sendMessage(ChatColor.RED + "Payment failed.");
            }
            return true;
        }

        if (cooldownSeconds > 0L) {
            lastPayMillis.put(player.getUniqueId(), System.currentTimeMillis());
        }

        if (messages != null) {
            messages.send(sender, "pay.sent", "player", target.getName(), "amount", MoneyUtil.format(amount));
            messages.send(target, "pay.received", "player", player.getName(), "amount", MoneyUtil.format(amount));
        } else {
            sender.sendMessage(ChatColor.GREEN + "You paid " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + " $" + MoneyUtil.format(amount) + ".");
            target.sendMessage(ChatColor.GREEN + "You received $" + MoneyUtil.format(amount) + " from " + ChatColor.WHITE + player.getName() + ChatColor.GREEN + ".");
        }
        return true;
    }
}
