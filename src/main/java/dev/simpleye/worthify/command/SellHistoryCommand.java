package dev.simpleye.worthify.command;

import dev.simpleye.worthify.gui.SellHistoryGuiManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SellHistoryCommand implements CommandExecutor {

    private final SellHistoryGuiManager gui;

    public SellHistoryCommand(SellHistoryGuiManager gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        int page = 1;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Usage: /sellhistory [page]");
                return true;
            }
        }

        gui.open(player, page);
        return true;
    }
}
