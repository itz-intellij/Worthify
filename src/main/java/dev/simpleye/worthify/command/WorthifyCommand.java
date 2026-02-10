package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class WorthifyCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public WorthifyCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();

        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            if (messages != null) {
                messages.send(sender, "worthify.help");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify Commands:");
                sender.sendMessage(ChatColor.GRAY + "/worthify help" + ChatColor.WHITE + " - Show this help");
                sender.sendMessage(ChatColor.GRAY + "/worthify version" + ChatColor.WHITE + " - Show plugin version");
                sender.sendMessage(ChatColor.GRAY + "/worthify reload" + ChatColor.WHITE + " - Reload configs");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            String version = plugin.getDescription().getVersion();
            if (messages != null) {
                messages.send(sender, "worthify.version", "version", version);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify v" + version);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            if (messages != null) {
                messages.send(sender, "worthify.reloaded");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify reloaded.");
            }
            return true;
        }

        if (messages != null) {
            messages.send(sender, "worthify.usage");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /worthify reload");
        }
        return true;
    }
}
