package dev.simpleye.worthify.command;

import dev.simpleye.worthify.gui.SellHistoryGuiManager;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SellHistoryCommand implements CommandExecutor {

    private final SellHistoryGuiManager gui;
    private final MessageService messages;

    public SellHistoryCommand(SellHistoryGuiManager gui, MessageService messages) {
        this.gui = gui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        int page = 1;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "sellhistory.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sellhistory [page]");
                }
                return true;
            }
        }

        gui.open(player, page);
        return true;
    }
}
