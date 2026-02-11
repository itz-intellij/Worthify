package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PayToggleCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public PayToggleCommand(WorthifyPlugin plugin) {
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

        boolean enabled = plugin.getPaySettingsStore().toggleReceiveEnabled(player.getUniqueId());
        if (messages != null) {
            messages.send(sender, enabled ? "pay.toggle_on" : "pay.toggle_off");
        } else {
            sender.sendMessage(enabled ? ChatColor.GREEN + "You can now receive payments." : ChatColor.RED + "You will no longer receive payments.");
        }
        return true;
    }
}
