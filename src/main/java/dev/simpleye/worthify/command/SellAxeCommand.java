package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.selltools.SellToolType;
import dev.simpleye.worthify.selltools.SellToolUtil;
import dev.simpleye.worthify.selltools.TimeUnitParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SellAxeCommand implements CommandExecutor {

    public static final String PERMISSION = "worthify.selltools.give";

    private final WorthifyPlugin plugin;
    private final SellToolUtil util;

    public SellAxeCommand(WorthifyPlugin plugin, SellToolUtil util) {
        this.plugin = plugin;
        this.util = util;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();

        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission("worthify.admin") && !sender.isOp()) {
            if (messages != null) {
                messages.send(sender, "errors.no_permission");
            } else {
                sender.sendMessage(ChatColor.RED + "No permission.");
            }
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            if (messages != null) {
                messages.send(sender, "selltools.axe.usage");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /sellaxe <time> <unit> [player]");
            }
            return true;
        }

        int time;
        try {
            time = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            if (messages != null) {
                messages.send(sender, "selltools.invalid_time", "time", args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid time: " + args[0]);
            }
            return true;
        }

        long duration = TimeUnitParser.parseDurationMillis(time, args[1]);
        if (duration <= 0L) {
            if (messages != null) {
                messages.send(sender, "selltools.invalid_unit", "unit", args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid unit: " + args[1]);
            }
            return true;
        }

        Player target;
        if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
        } else {
            target = sender instanceof Player p ? p : null;
        }

        if (target == null) {
            if (messages != null) {
                messages.send(sender, "selltools.player_required");
            } else {
                sender.sendMessage(ChatColor.RED + "Player is required when using this from console.");
            }
            return true;
        }

        if (!util.isEnabled(SellToolType.AXE)) {
            if (messages != null) {
                messages.send(sender, "selltools.disabled");
            } else {
                sender.sendMessage(ChatColor.RED + "Sell tools are disabled.");
            }
            return true;
        }

        long expiresAt = System.currentTimeMillis() + duration;
        ItemStack item = util.createTool(SellToolType.AXE, expiresAt);
        target.getInventory().addItem(item);

        if (messages != null) {
            messages.send(sender, "selltools.given", "player", target.getName());
        }
        return true;
    }
}
